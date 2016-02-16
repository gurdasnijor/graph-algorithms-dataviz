(ns customer-network.core
  (:require [clojure.math.numeric-tower :as math]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]))

(defn load-edges
  "Reads a file vertically delimited by newlines, and horizontally by a space character"
  [filename]
  (into []
    (let [edges (with-open [in-file (io/reader filename)]
      (doall
        (csv/read-csv in-file :separator \space)))]
      (for [[o d] edges] [(keyword o) (keyword d)]))))

(defn build-graph
  "Produces a map of vertices to their adjacent vertices"
  [edges]
    (reduce (fn [m [v1 v2]]
      (assoc-in m [v1 v2] 1)) {} (concat edges (map reverse edges))))

(defn update-costs
  "Returns costs updated with any shorter paths found to curr's unvisisted
  neighbors by using curr's shortest path"
  [g costs unvisited curr]
  (let [curr-cost (get costs curr)]
    (reduce-kv
      (fn [c nbr nbr-cost]
        (if (unvisited nbr)
          (update-in c [nbr] min (+ curr-cost nbr-cost))
          c))
      costs
      (get g curr))))

(defn dijkstra
  "Returns a map of nodes to minimum cost from src using Dijkstra algorithm.
  Graph is a map of nodes to map of neighboring nodes and associated cost.
  Optionally, specify destination node to return once cost is known"
  ([g src]
    (dijkstra g src nil))
  ([g src dst]
    (loop [costs (assoc (zipmap (keys g) (repeat Float/POSITIVE_INFINITY)) src 0)
           curr src
           unvisited (disj (apply hash-set (keys g)) src)]
      (cond
       (= curr dst)
       (select-keys costs [dst])
       (or (empty? unvisited) (= Float/POSITIVE_INFINITY (get costs curr)))
       costs
       :else
       (let [next-costs (update-costs g costs unvisited curr)
         next-node (apply min-key next-costs unvisited)]
         (recur next-costs next-node (disj unvisited next-node)))))))

(defn get-fraud-coefficient
  "Accepts a distance-map of shortest paths relative to a given vertex along with
  a fraudulent-customers vector of customer ids associated with fraud"
  [distance-map fraudulent-customers]
  (->> (select-keys distance-map fraudulent-customers)
       vals
       (map #(- 1 (math/expt (/ 1 2) %)))
       (reduce *)))

(defn get-vertex-score
  "Get the final score of a vertex given the graph produced by build-graph and a
  fraudulent-customer vector"
  [graph vertex fraudulent-customers]
  (let [distance-map (dijkstra graph vertex)
        initial-score (/ 1 (reduce + (vals distance-map)))
        fraud-coefficient (get-fraud-coefficient distance-map fraudulent-customers)]
    (* fraud-coefficient initial-score)))

(defn score-map
  "Get a map of vertices (customers) to their corresponding scores"
  [edges fraudulent-customers]
  (let [graph (build-graph edges)]
    (reduce-kv
      (fn [vert-close-map vertex _]
          (assoc vert-close-map vertex (get-vertex-score graph vertex fraudulent-customers))) {} graph)))
