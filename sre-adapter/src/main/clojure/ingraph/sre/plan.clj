(ns ingraph.sre.plan
    "Contains the main configuration of constraints, operations and others
    available in ingraph"
    (:require [sre.plan.dsl.constraint :refer [defconstraint]]
      [sre.plan.dsl.op :refer [defop]]
      [sre.plan.dsl.config :refer :all])
    (:gen-class :name ingraph.sre.Plan :main false))

(defconfig Ingraph)

(defconstraint Known [known])
(defconstraint Element [element] :implies Known [element])
(defconstraint Edge [edge] :implies Element [edge])
(defconstraint Vertex [vertex] :implies Element [vertex])
(defconstraint HasLabels [vertex labels] :implies Vertex [vertex] Known [type])
(defconstraint HasType [edge type] :implies Edge [edge] Known [type])
(defconstraint Property [element key value] :implies Element [element])
(defconstraint DirectedEdge [source edge target] :implies
               Vertex [source]
               Edge [edge]
               Vertex [target])
(defconstraint Equals [x y] :implies Known [x] Known [y])

(defop GetVertices [vertex]
       :satisfies Vertex [vertex])
(defop GetVerticesByLabels [vertex labels]
       :requires Known [labels]
       :satisfies Vertex [vertex] HasLabels [vertex labels])
(defop CheckLabels [vertex labels]
       :requires Vertex [vertex] Known [labels]
       :satisfies HasLabels [vertex labels])
(defop GetEdges [source edge target]
       :satisfies DirectedEdge [source edge target])
(defop GetEdgesByType [source edge target type]
       :requires Known [type]
       :satisfies DirectedEdge [source edge target] HasType [edge type])
(defop CheckType [edge type]
       :requires Edge [edge] Known [type]
       :satisfies HasType [edge type])
(defop ExtendOut [source edge target]
       :requires Vertex [source]
       :satisfies DirectedEdge [source edge target])
(defop ExtendIn [target edge source]
       :requires Vertex [target]
       :satisfies DirectedEdge [source edge target])
(defop ExtendOutByType [source edge target type]
       :requires Vertex [source] Known [type]
       :satisfies DirectedEdge [source edge target] HasType [edge type])
(defop ExtendInByType [target edge source type]
       :requires Vertex [target] Known [type]
       :satisfies DirectedEdge [source edge target] HasType [edge type])
(defop Join [source edge target]
       :requires Vertex [source] Vertex [target]
       :satisfies DirectedEdge [source edge target])
(defop JoinByType [source edge target type]
       :requires Vertex [source] Vertex [target] Known [type]
       :satisfies DirectedEdge [source edge target] HasType [edge type])
(defop CheckDirectedEdge [source edge target]
       :requires Vertex [source] Edge [edge] Vertex [target]
       :satisfies DirectedEdge [source edge target])
(defop CheckDirectedEdgeByType [source edge target type]
       :requires Vertex [source] Edge [edge] Vertex [target] Known [type]
       :satisfies DirectedEdge [source edge target] HasType [edge type])
(defop EvalEquals [x y]
       :requires Known [x] Known [y]
       :satisfies Equals [x y])
