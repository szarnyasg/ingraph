package hu.bme.mit.ire.nodes.unary

import hu.bme.mit.ire.messages.{ChangeSet, ReteMessage, SingleForwarder}

/**
  * Currently used for selecting elements from a list.
  *
  * @param next
  * @param function
  * @param index
  * @param expectedTerminatorCount
  */
class MapperNode(override val next: (ReteMessage) => Unit,
                 val function: (Any) => Any, val index: Int,
                 override val expectedTerminatorCount: Int = 1
                ) extends UnaryNode with SingleForwarder {
  override def onSizeRequest() = 0

  def onChangeSet(changeSet: ChangeSet): Unit = {
    forward(ChangeSet(
      changeSet.positive.map(t => t.updated(index, function(t(index)))),
      changeSet.negative.map(t => t.updated(index, function(t(index))))
    ))
  }
}
