(ns sre.main
  "Contains the main configuration of constraints, operations and others
  available in the engine"
  (:require [sre.constraint :refer [defconstraint]]
            [sre.op :refer [defop]]))

(defconstraint Element [element])
(defconstraint Edge [edge] :implies Element [edge])
(defconstraint Vertex [vertex] :implies Element [vertex])
(defconstraint HasLabels [vertex labels] :implies Vertex [vertex])
(defconstraint HasType [edge type] :implies Edge [edge])
(defconstraint Property [element key value] :implies Element [element])
(defconstraint DirectedEdge [source edge target] :implies
               Vertex [source]
               Edge [edge]
               Vertex [target])
(defconstraint Assert1 [x expr] :implies Element [x])
(defconstraint Assert2 [x y expr] :implies Element [x] Element [y])

(defop GetVertices [vertex]
       :satisfies Vertex [vertex])
(defop GetVerticesByLabels [vertex labels]
       :satisfies Vertex [vertex] HasLabels [vertex labels])
(defop GetEdges [source edge target]
       :satisfies DirectedEdge [source edge target])
(defop GetEdgesByType [source edge target type]
       :satisfies DirectedEdge [source edge target] HasType [edge type])
(defop ExtendOut [source edge target]
       :requires Vertex [source]
       :satisfies DirectedEdge [source edge target])
(defop ExtendIn [source edge target]
       :requires Vertex [target]
       :satisfies DirectedEdge [target edge source])
(defop ExtendOutByType [source edge target type]
       :requires Vertex [source]
       :satisfies DirectedEdge [source edge target] HasType [edge type])
(defop ExtendInByType [source edge target type]
       :requires Vertex [target]
       :satisfies DirectedEdge [target edge source] HasType [edge type])
(defop EvalAssert1 [x expr]
       :requires Element [x]
       :satisfies Assert1 [x expr])
(defop EvalAssert2 [x y expr]
       :requires Element [x] Element [y]
       :satisfies Assert2 [x expr])