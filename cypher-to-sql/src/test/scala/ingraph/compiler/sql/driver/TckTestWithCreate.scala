package ingraph.compiler.sql.driver

import ingraph.compiler.sql.driver.TckTestWithCreate.scenarioSet
import ingraph.tck.{TckScenarioSet, TckTestRunner}
import org.scalatest.FunSuite

class TckTestWithCreate extends FunSuite with TckTestRunner with SharedSqlDriver {
  runTckTests(() => new TckAdapter(session), scenarioSet)
}

object TckTestWithCreate {
  val selectedFeatures: Set[String] = Set(
    "AggregationAcceptance",
    "ColumnNameAcceptance",
    "Comparability",
    "EqualsAcceptance",
    "FunctionsAcceptance",
    "LabelsAcceptance",
    "LargeIntegerEquality",
    "Literals",
    "Local",
    "MatchAcceptance",
    "MatchAcceptance2",
    "MatchingSelfRelationships",
    "OptionalMatch",
    "OptionalMatchAcceptance",
    "OrderByAcceptance",
    "ReturnAcceptance2",
    "ReturnAcceptanceTest",
    "StartingPointAcceptance",
    "TernaryLogicAcceptanceTest",
    "TriadicSelection",
    "ValueHashJoinAcceptance",
    "VarLengthAcceptance",
    "WithAcceptance",
    ""
  ).filter(!_.isEmpty)

  val selectedScenarios: Set[String] = Set(
    "Aggregate on property",
    "Count nodes",
    "Count non-null values",
    "Distinct on null",
    "Distinct on unbound node",
    "Keeping used expression 1",
    "Comparing strings and integers using > in a OR'd predicate",
    "Comparing strings and integers using > in an AND'd predicate",
    "Comparing nodes to nodes",
    "Comparing relationships to relationships",
    "Run coalesce",
    "`exists()` is case insensitive",
    "`type()`",
    "`type()` on mixed null and non-null relationships",
    "`type()` on null relationship",
    "`type()` on two relationships",
    "Using `labels()` in return clauses",
    "Does not lose precision",
    "Handling explicit equality of large integer",
    "Handling explicit equality of large integer, non-equal values",
    "Handling inlined equality of large integer",
    "Handling inlined equality of large integer, non-equal values",
    "Return a boolean",
    "Return a double-quoted string",
    "Return a float",
    "Return a nonempty list",
    "Return a single-quoted string",
    "Return an integer",
    "Return null",
    "Generic CASE",
    "Return vertices and edges",
    "Return vertices and edges with integer properties",
    "Simple CASE",
    "Unnamed columns",
    "Cope with shadowed variables",
    "Filter based on rel prop name",
    "Filter out based on node prop name",
    "Get neighbours",
    "Get related to related to",
    "Get two related nodes",
    "Handle OR in the WHERE clause",
    "Handle comparison between node properties",
    "Honour the column name for RETURN items",
    "Rel type function works as expected",
    "Return two subgraphs with bound undirected relationship",
    "Return two subgraphs with bound undirected relationship and optional relationship",
    "Use multiple MATCH clauses to do a Cartesian product",
    "Use params in pattern matching predicates",
    "Walk alternative relationships",
    "Comparing nodes for equality",
    "Do not fail when evaluating predicates with illegal operations if the AND'ed predicate evaluates to false",
    "Do not fail when evaluating predicates with illegal operations if the OR'd predicate evaluates to true",
    "Do not fail when predicates on optionally matched and missed nodes are invalid",
    "Do not return non-existent nodes",
    "Do not return non-existent relationships",
    "Handling cyclic patterns",
    "Handling cyclic patterns when separated into two parts",
    "MATCH with OPTIONAL MATCH in longer pattern",
    "Matching a relationship pattern using a label predicate",
    "Matching a relationship pattern using a label predicate on both sides",
    "Matching all nodes",
    "Matching and returning ordered results, with LIMIT",
    "Matching disconnected patterns",
    "Matching from null nodes should return no results owing to finding no matches",
    "Matching from null nodes should return no results owing to matches being filtered out",
    "Matching nodes using multiple labels",
    "Matching nodes with many labels",
    "Matching using a relationship that is already bound",
    "Matching using a relationship that is already bound, in conjunction with aggregation",
    "Matching using a simple pattern with label predicate",
    "Matching using relationship predicate with multiples of the same type",
    "Matching with aggregation",
    "Matching with many predicates and larger pattern",
    "Missing node property should become null",
    "Missing relationship property should become null",
    //    "Multiple anonymous nodes in a pattern",
    "Non-optional matches should not return nulls",
    "OPTIONAL MATCH returns null",
    "OPTIONAL MATCH with previously bound nodes",
    "ORDER BY with LIMIT",
    "Optionally matching from null nodes should return null",
    "Projecting nodes and relationships",
    "Returning a node property value",
    "Returning a relationship property value",
    "Returning bound nodes that are not part of the pattern",
    "Returning label predicate expression",
    "Simple OPTIONAL MATCH on empty graph",
    "Simple node property predicate",
    "Simple variable length pattern",
    "Three bound nodes pointing to the same node",
    "Three bound nodes pointing to the same node with extra connections",
    "Two bound nodes pointing to the same node",
    "Variable length pattern checking labels on endnodes",
    "Variable length pattern with label predicate on both sides",
    "Zero-length variable length pattern in the middle of the pattern",
    "Directed match of a simple relationship",
    "Directed match of a simple relationship, count",
    "Directed match on self-relationship graph",
    "Directed match on self-relationship graph, count",
    "Undirected match on simple relationship graph",
    "Undirected match on simple relationship graph, count",
    "Satisfies the open world assumption, relationships between different nodes",
    "Satisfies the open world assumption, relationships between same nodes",
    "Satisfies the open world assumption, single relationship",
    "Handling correlated optional matches; first does not match implies second does not match",
    "Handling optional matches between nulls",
    "Handling optional matches between optionally matched entities",
    "Longer pattern with bound nodes",
    "Longer pattern with bound nodes without matches",
    "OPTIONAL MATCH and bound nodes",
    "OPTIONAL MATCH with labels on the optional end node",
    "Respect predicates on the OPTIONAL MATCH",
    "Return null when no matches due to inline label predicate",
    //    "Return null when no matches due to label predicate in WHERE",
    "Variable length optional relationships",
    "Variable length optional relationships with bound nodes",
    "Variable length optional relationships with length predicates",
    "WITH after OPTIONAL MATCH",
    "Handle ORDER BY with LIMIT 1",
    "ORDER BY with LIMIT 0 should not generate errors",
    "Accept valid Unicode literal",
    "Aliasing expressions",
    "DISTINCT on nullable values",
    "Ordering with aggregation",
    "Returned columns do not change from using ORDER BY",
    "Returning an expression",
    "Using aliased DISTINCT expression in ORDER BY",
    "Get rows in the middle",
    "Limit to two hits with explicit order",
    "Start the result from the second row",
    "Support column renaming",
    "Support ordering by a property after being distinct-ified",
    "Support sort and distinct",
    "Find all nodes",
    "Find labelled nodes",
    "Find nodes by property",
    "It is unknown - i.e. null - if a null is equal to a null",
    "It is unknown - i.e. null - if a null is not equal to a null",
    "Handling triadic friend of a friend",
    "Find friends of others",
    "Handling a variable length relationship and a standard relationship in chain, longer 1",
    "Handling a variable length relationship and a standard relationship in chain, longer 2",
    "Handling a variable length relationship and a standard relationship in chain, single length 1",
    "Handling a variable length relationship and a standard relationship in chain, single length 2",
    "Handling a variable length relationship and a standard relationship in chain, zero length 1",
    "Handling a variable length relationship and a standard relationship in chain, zero length 2",
    "Handling explicitly unbounded variable length match",
    "Handling lower bounded variable length match 1",
    "Handling lower bounded variable length match 2",
    "Handling lower bounded variable length match 3",
    "Handling single bounded variable length match 1",
    "Handling single bounded variable length match 2",
    "Handling single bounded variable length match 3",
    "Handling symmetrically bounded variable length match, bounds are one",
    "Handling symmetrically bounded variable length match, bounds are two",
    "Handling symmetrically bounded variable length match, bounds are zero",
    "Handling unbounded variable length match",
    "Handling upper and lower bounded variable length match 1",
    "Handling upper and lower bounded variable length match 2",
    "Handling upper and lower bounded variable length match, empty interval 1",
    "Handling upper and lower bounded variable length match, empty interval 2",
    "Handling upper bounded variable length match 1",
    "Handling upper bounded variable length match 2",
    "Handling upper bounded variable length match, empty interval",
    "Aliasing",
    "Multiple WITHs using a predicate and aggregation",
    "No dependencies between the query parts",
    "ORDER BY and LIMIT can be used",
    "Single WITH using a predicate and aggregation",
    "WHERE after WITH can filter on top of an aggregation",
    "WHERE after WITH should filter results",
    "Dependant CREATE with single row",
    "Dependant CREATE with single row - with aliased attribute",
    ""
  ).filter(!_.isEmpty)

  val ignoredScenarios: Set[String] = Set(
    "Many CREATE clauses",
    "Generate the movie graph correctly",
    "Returning multiple node property values",
    // placeholder
    ""
  ).filter(!_.isEmpty)

  val scenarioSet = new TckScenarioSet(selectedFeatures, ignoredScenarios, selectedScenarios)
}