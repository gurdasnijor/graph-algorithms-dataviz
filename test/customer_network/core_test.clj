(ns customer-network.core-test
  (:require [clojure.test :refer :all]
            [customer-network.core :refer :all]))

(deftest graph-algorithms
  (testing "build-graph"
    (let [edges [[:1 :2] [:2 :4] [:2 :3] [:3 :5] [:4 :5] [:5 :6]]]
      (is (= (build-graph edges) {:1 {:2 1}
                                  :2 {:1 1 :3 1 :4 1}
                                  :3 {:2 1 :5 1}
                                  :4 {:2 1 :5 1}
                                  :5 {:3 1 :4 1 :6 1}
                                  :6 {:5 1}}))))

  (testing "dijkstra"
    (let [edges [[:1 :2] [:2 :4] [:2 :3] [:3 :5] [:4 :5] [:5 :6]]
          graph (build-graph edges)]
      (is (= (dijkstra graph :1) {:1 0 :2 1 :3 2 :4 2 :5 3 :6 4}))
      (is (= (dijkstra graph :2) {:1 1 :2 0 :3 1 :4 1 :5 2 :6 3}))
      (is (= (dijkstra graph :3) {:1 2 :2 1 :3 0 :4 2 :5 1 :6 2})))))

(deftest scoring-algorithms
  (testing "get-fraud-coefficient"
    (let [edges [[:1 :2] [:2 :4] [:2 :3] [:3 :5] [:4 :5] [:5 :6]]
          graph (build-graph edges)
          distance-map (dijkstra graph :1)]
      (is (= (get-fraud-coefficient distance-map [:1]) 0))
      (is (= (get-fraud-coefficient distance-map [:2]) 1/2))
      (is (= (get-fraud-coefficient distance-map [:3]) 3/4))
      (is (= (get-fraud-coefficient distance-map [:3 :4]) 9/16))))

  (testing "get-vertex-score"
    (let [edges [[:1 :2] [:2 :4] [:2 :3] [:3 :5] [:4 :5] [:5 :6]]
          graph (build-graph edges)]
      (is (= (get-vertex-score graph :1 []) 1/12))
      (is (= (get-vertex-score graph :1 [:1]) 0))
      (is (= (get-vertex-score graph :1 [:2]) 1/24))
      (is (= (get-vertex-score graph :1 [:3]) 1/16))
      (is (= (get-vertex-score graph :1 [:3 :4]) 3/64))))

  (testing "score-map"
    (let [edges [[:1 :2] [:2 :4] [:2 :3] [:3 :5] [:4 :5] [:5 :6]]]
      (is (= (score-map edges []) {:1 1/12 :2 1/8 :3 1/8 :4 1/8 :5 1/8 :6 1/12}))
      (is (= (score-map edges [:1]) {:1 0N :2 1/16 :3 3/32 :4 3/32 :5 7/64 :6 5/64}))
      (is (= (score-map edges [:2]) {:1 1/24 :2 0N :3 1/16 :4 1/16 :5 3/32 :6 7/96}))
      (is (= (score-map edges [:2 :3]) {:1 1/32 :2 0N :3 0N :4 3/64 :5 3/64 :6 7/128})))))
