package ingraph.compiler.sql

import ingraph.compiler.sql.CompileSql.{getQuotedColumnName, getSingleQuotedString, _}
import ingraph.compiler.sql.IndentationPreservingStringInterpolation._
import ingraph.model.expr.{EdgeListAttribute, NodeHasLabelsAttribute}
import ingraph.model.fplan
import ingraph.model.fplan.{BinaryFNode, FNode, LeafFNode, UnaryFNode}
import ingraph.model.treenodes.{GenericBinaryNode, GenericLeafNode, GenericUnaryNode}
import org.apache.spark.sql.catalyst.expressions.{Attribute, Literal}
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan

trait SqlNode extends LogicalPlan {
  // TODO remove
  val options: CompilerOptions = CompilerOptions()

  override def children: Seq[SqlNode]

  override def output: Seq[Attribute] = ???

  def sql: String
}

object SqlNode {
  def apply(fNode: fplan.GetEdges): GetEdges =
    GetEdges(fNode)

  def apply(fNode: FNode): SqlNode with WithFNodeOrigin[FNode] =
    fNode match {
      case fNode: BinaryFNode => {
        val left = SqlNode(fNode.left)
        val right = SqlNode(fNode.right)

        fNode match {
          case fNode: fplan.AntiJoin => AntiJoin(fNode, left, right)
          // transform right child here to keep type information
          case fNode: fplan.TransitiveJoin => TransitiveJoin(fNode, left, apply(fNode.right))
          // EquiJoinLike nodes except TransitiveJoin
          case fNode: fplan.EquiJoinLike => EquiJoinLike(fNode, left, right)
        }
      }
      case fNode: UnaryFNode => {
        val child = SqlNode(fNode.child)

        fNode match {
          case fNode: fplan.SortAndTop => SortAndTop(fNode, child)
          case fNode: fplan.Selection => Selection(fNode, child)
          case fNode: fplan.Production => Production(fNode, child)
          case fNode: fplan.Projection => Projection(fNode, child)
          case fNode: fplan.DuplicateElimination => DuplicateElimination(fNode, child)
          case fNode: fplan.Grouping => Grouping(fNode, child)
          case fNode: fplan.AllDifferent => AllDifferent(fNode, child)
        }
      }

      case fNode: LeafFNode => {
        fNode match {
          case fNode: fplan.Dual => Dual(fNode)
          case fNode: fplan.GetVertices => GetVertices(fNode)
          case fNode: fplan.GetEdges => apply(fNode)
        }
      }
    }
}

trait WithFNodeOrigin[+T <: FNode] {
  def fNode: T
}

abstract class LeafSqlNode[+T <: LeafFNode] extends GenericLeafNode[SqlNode] with SqlNode with WithFNodeOrigin[T] {}

abstract class UnarySqlNode[+T <: UnaryFNode](
                                               val child: SqlNode with WithFNodeOrigin[FNode])
  extends GenericUnaryNode[SqlNode] with SqlNode with WithFNodeOrigin[T] {

  val childSql = child.sql
}

abstract class BinarySqlNode[+T <: BinaryFNode](val left: SqlNode with WithFNodeOrigin[FNode],
                                                val right: SqlNode with WithFNodeOrigin[FNode])
  extends GenericBinaryNode[SqlNode] with SqlNode with WithFNodeOrigin[T] {

  def leftSql = left.sql

  def rightSql = right.sql
}

case class GetEdges(fNode: fplan.GetEdges) extends LeafSqlNode[fplan.GetEdges] {
  override def sql: String = getGetEdgesSql(fNode)
}

case class GetVertices(fNode: fplan.GetVertices) extends LeafSqlNode[fplan.GetVertices] {
  val columns =
    (
      s"""vertex_id AS ${getQuotedColumnName(fNode.nnode.v)}""" +:
        fNode.requiredProperties
          // skip NodeHasLabelsAttribute since it has no resolvedName to use as column name
          // TODO use NodeHasLabelsAttribute
          .filterNot(_.isInstanceOf[NodeHasLabelsAttribute])
          .map(prop =>s"""(SELECT value FROM vertex_property WHERE parent = vertex_id AND key = ${getSingleQuotedString(prop.name)}) AS ${getQuotedColumnName(prop)}"""))
      .mkString(",\n")

  val labelConstraint = getVertexLabelSqlCondition(fNode.nnode.v.labels, "vertex_id")
    .map("WHERE " + _)
    .getOrElse("")

  override def sql: String =
    i"""SELECT
       |  $columns
       |FROM vertex
       |$labelConstraint"""
}

case class TransitiveJoin(fNode: fplan.TransitiveJoin,
                          override val left: SqlNode with WithFNodeOrigin[FNode],
                          override val right: GetEdges)
  extends BinarySqlNode[fplan.TransitiveJoin](left, right) {
  val edgesNode = right.fNode

  // TODO override column names in right
  override val rightSql: String = getGetEdgesSql(edgesNode, Some("current_from"), Some("edge_id"))
  val edgesSql = rightSql

  val edgeListAttribute = fNode.nnode.edgeList
  val edgeListName = getQuotedColumnName(edgesNode.nnode.edge)
  val edgesFromVertexName = getQuotedColumnName(edgesNode.nnode.src)
  val edgesToVertexName = getQuotedColumnName(edgesNode.nnode.trg)
  val leftNodeColumnNames = left.fNode.flatSchema.map(getQuotedColumnName).mkString(", ")
  val joinNodeColumnNames = fNode.flatSchema.map(getQuotedColumnName).mkString(",\n")

  val lowerBound = edgeListAttribute.minHops.getOrElse(1)
  val lowerBoundConstraint =
    if (lowerBound != 0)
      s"WHERE array_length($edgeListName) >= $lowerBound"
    else
      ""

  val upperBoundConstraint =
    if (edgeListAttribute.maxHops.isDefined)
      s"AND array_length($edgeListName) < ${edgeListAttribute.maxHops.get}"
    else
      ""

  override def sql: String =
    i"""WITH RECURSIVE recursive_table AS (
       |  (
       |    WITH left_query AS (
       |      $leftSql
       |    )
       |    SELECT
       |      *,
       |      ARRAY [] :: integer [] AS $edgeListName,
       |      $edgesFromVertexName AS next_from,
       |      $edgesFromVertexName AS $edgesToVertexName
       |    FROM left_query
       |  )
       |  UNION ALL
       |  SELECT
       |    $leftNodeColumnNames,
       |    ($edgeListName|| edge_id) AS $edgeListName,
       |    edges.$edgesToVertexName AS nextFrom,
       |    edges.$edgesToVertexName
       |  FROM
       |    ( $edgesSql
       |    ) edges
       |    INNER JOIN recursive_table
       |      ON edge_id <> ALL ($edgeListName) -- edge uniqueness
       |         AND next_from = current_from
       |         $upperBoundConstraint
       |)
       |SELECT
       |  $joinNodeColumnNames
       |FROM recursive_table
       |$lowerBoundConstraint"""
}

// EquiJoinLike nodes except TransitiveJoin
case class EquiJoinLike(fNode: fplan.EquiJoinLike,
                        override val left: SqlNode with WithFNodeOrigin[FNode],
                        override val right: SqlNode with WithFNodeOrigin[FNode])
  extends BinarySqlNode[fplan.EquiJoinLike](left, right) {

  val leftColumns = fNode.left.flatSchema.map(getQuotedColumnName)
  val rightColumns = fNode.right.flatSchema.map(getQuotedColumnName)
  // prefer columns from  the left query, because in left outer join the right ones may contain NULL
  val resultColumns =
    (leftColumns.map("left_query." + _) ++
      (rightColumns.toSet -- leftColumns).map("right_query." + _))
      .mkString(", ")

  val columnConditions = fNode.commonAttributes
    .map(getQuotedColumnName)
    .map(name => s"left_query.$name = right_query.$name")
  val joinConditions = fNode match {
    case node: fplan.ThetaLeftOuterJoin => columnConditions :+ getSql(node.nnode.condition, options)
    case _ => columnConditions
  }

  val joinConditionPart = if (joinConditions.isEmpty)
    "ON TRUE"
  else
    i"ON ${joinConditions.mkString(" AND\n")}"

  val joinType = fNode match {
    case _: fplan.Join => "INNER"
    case _: fplan.LeftOuterJoin => "LEFT OUTER"
    case _: fplan.ThetaLeftOuterJoin => "LEFT OUTER"
  }

  override def sql: String =
    i"""SELECT $resultColumns FROM
       |  ( $leftSql
       |  ) left_query
       |  $joinType JOIN
       |  ( $rightSql
       |  ) right_query
       |$joinConditionPart"""
}

case class AntiJoin(fNode: fplan.AntiJoin,
                    override val left: SqlNode with WithFNodeOrigin[FNode],
                    override val right: SqlNode with WithFNodeOrigin[FNode])
  extends BinarySqlNode[fplan.AntiJoin](left, right) {
  val columnConditions = fNode.commonAttributes
    .map(getQuotedColumnName)
    .map(name => s"left_query.$name = right_query.$name")

  val conditionPart = if (columnConditions.isEmpty)
    "TRUE"
  else
    columnConditions.mkString(" AND\n")

  override def sql: String =
    i"""SELECT * FROM
       |  ( $leftSql
       |  ) left_query
       |WHERE NOT EXISTS(
       |  SELECT * FROM
       |    ( $rightSql
       |    ) right_query
       |  WHERE $conditionPart
       |  )"""
}

case class Production(fNode: fplan.Production,
                      override val child: SqlNode with WithFNodeOrigin[FNode])
  extends UnarySqlNode[fplan.Production](child) {
  val renamePairs = fNode.output.zip(fNode.outputNames)
    .map {
      case (attribute, outputName) =>
        convertAttributeAtProductionNode(attribute) -> getQuotedColumnName(outputName)
    }

  override def sql: String =
    getProjectionSql(fNode, renamePairs, childSql, options)
}

case class Projection(fNode: fplan.Projection,
                      override val child: SqlNode with WithFNodeOrigin[FNode])
  extends UnarySqlNode[fplan.Projection](child) {
  val renamePairs = fNode.flatSchema.map(proj => (getSql(proj, options), getQuotedColumnName(proj)))

  override def sql: String =
    getProjectionSql(fNode, renamePairs, childSql, options)
}

case class Selection(fNode: fplan.Selection,
                     override val child: SqlNode with WithFNodeOrigin[FNode])
  extends UnarySqlNode[fplan.Selection](child) {
  val condition = getSql(fNode.nnode.condition, options)

  override def sql: String =
    i"""SELECT * FROM
       |  (
       |    $childSql
       |  ) subquery
       |WHERE $condition"""
}

case class AllDifferent(fNode: fplan.AllDifferent,
                        override val child: SqlNode with WithFNodeOrigin[FNode])
  extends UnarySqlNode[fplan.AllDifferent](child) {
  val edgeIdsArray = ("ARRAY[]::INTEGER[]" +: fNode.nnode.edges.map(getQuotedColumnName)).mkString(" || ")
  // only more than 1 node must be checked for uniqueness (edge list can contain more edges)
  val allDifferentNeeded = fNode.nnode.edges.size > 1 || fNode.nnode.edges.exists(_.isInstanceOf[EdgeListAttribute])

  override def sql: String =
    if (allDifferentNeeded)
      i"""SELECT * FROM
         |  (
         |    $childSql
         |  ) subquery
         |  WHERE is_unique($edgeIdsArray)"""
    else
      s"""   -- noop
         |$childSql""".stripMargin
}

case class SortAndTop(fNode: fplan.SortAndTop,
                      override val child: SqlNode with WithFNodeOrigin[FNode])
  extends UnarySqlNode[fplan.SortAndTop](child) {
  val nnode = fNode.nnode

  val orderByParts = nnode.order.map(order => getSql(order.child, options) + " " + order.direction.sql + " " + order.nullOrdering.sql)
  val limitPart = nnode.limitExpr.map { case Literal(num: Long, _) => s"LIMIT $num" }.getOrElse("")
  val offsetPart = nnode.skipExpr.map { case Literal(num: Long, _) => s"OFFSET $num" }.getOrElse("")

  override def sql: String =
    i"""SELECT * FROM
       |  (
       |    $childSql
       |  ) subquery
       |  ORDER BY ${orderByParts.mkString(", ")}
       |  $limitPart
       |  $offsetPart"""
}

case class DuplicateElimination(fNode: fplan.DuplicateElimination,
                                override val child: SqlNode with WithFNodeOrigin[FNode])
  extends UnarySqlNode[fplan.DuplicateElimination](child) {
  override def sql: String =
    i"""SELECT DISTINCT * FROM
       |  (
       |    $childSql
       |  ) subquery"""
}

case class Grouping(fNode: fplan.Grouping,
                    override val child: SqlNode with WithFNodeOrigin[FNode])
  extends UnarySqlNode[fplan.Grouping](child) {
  val renamePairs = fNode.flatSchema.map(proj => (getSql(proj, options), getQuotedColumnName(proj)))
  val projectionSql = getProjectionSql(fNode, renamePairs, childSql, options)
  val groupByColumns = (fNode.nnode.aggregationCriteria ++ fNode.requiredProperties).map(getSql(_, options))
  val groupByPart = if (groupByColumns.isEmpty)
    ""
  else
    "GROUP BY " + groupByColumns.mkString(", ")

  override def sql: String =
    i"""$projectionSql
       |$groupByPart"""
}

case class Dual(fNode: fplan.Dual) extends LeafSqlNode[fplan.Dual] {
  override def sql: String = "SELECT"
}







