(ns sre.plan.dsl.constraint
  (:require [clojure.set :refer :all]
            [clojure.algo.generic.functor :refer :all]))

(defn- iter-implications [implications]
  (let [[constraint vars & rest] implications]
    (if (some? constraint)
      `(~@(iter-implications rest) [#'~constraint ~@(map keyword vars)])
      `())))

(defn bind
  "Binds constraint parameters to the given arguments"
  [constraint & args]
  (let [lkp (zipmap (:vars constraint) args)]
    (-> constraint
        (update-in [:vars] (partial fmap #(lkp %1)))
        (update-in [:implies] (partial fmap (fn [[constraint & params]]
                                              (let [args (map #(lkp %1) params)]
                                                (into [] (cons constraint args)))))))))

(defn implies
  "Returns a set of direct implications"
  [constraint]
  (into #{} (map (fn [[constraint & args]] (apply bind (cons @constraint args))) (:implies constraint))))

(defn implies*
  "Returns closure of implications"
  [constraint]
  (loop [unresolved #{constraint} result #{}]
    (if (empty? unresolved)
      result
      (let [[first & rest] (into () unresolved)]
        (recur (union (implies first) (into #{} rest)) (conj result first))))))


(defmacro defconstraint
  "Let's you define a constraint like a boss.

  Usage:
    (defconstraint MyConstraint [& vars] :implies* constraint-argument-pairs*)

  Examples:
    (defconstraint Element [element])
    (defconstraint Vertex [vertex] :implies Element[vertex])
    (defconstraint PowerPuffGirls [sugar spice everything-nice]
        :implies
          Element [sugar]
          Element [spice]
          Element [everything-nice]
          Mix [sugar spice everything-nice]"
  [name vars & rest]
  (let [prefix# (str name "Factory-")]
    `(do
       (def ~name {:name    #'~name
                   :vars    [~@(map keyword vars)]
                   :arity   ~(count vars)
                   :implies #{~@(let [[implies-kw# & implications#] rest]
                                  (case implies-kw#
                                    nil `()
                                    :implies (iter-implications implications#)))}})
       (defn ~(symbol (str prefix# "create")) [~@vars] (bind ~name ~@vars))
       (gen-class :name ~(str *ns* "." name "Factory")
                  :prefix ~prefix#
                  :methods [^:static [~'create [~@(repeat (count vars) Integer)] Object]])
       ~(if (contains? (ns-interns *ns*) 'constraints)
          `(def ~'constraints (conj ~'constraints ~name))))))

