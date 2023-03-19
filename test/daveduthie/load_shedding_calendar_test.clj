(ns daveduthie.load-shedding-calendar-test
  (:require [clojure.test :refer [deftest is testing]]
            [daveduthie.load-shedding-calendar :as sut])
  (:import (java.time ZonedDateTime)))

(defn intervals
  [& intervals]
  (let [now (ZonedDateTime/now)]
    (map (fn [[start end]]
           {:start (.plusHours now start), :end (.plusHours now end)})
      intervals)))

(deftest intersection-test
  (testing "identical intervals intersect"
    (let [[a] (intervals [0 2])] (is (sut/intersection a a))))
  (let [[a b] (intervals [0 2] [1 3])]
    (testing "1 overlaps 2 from the left" (is (sut/intersection a b)))
    (testing "1 overlaps 2 from the right" (is (sut/intersection b a))))
  (let [[a b] (intervals [1 2] [0 3])]
    (testing "1 is contained within 2" (is (sut/intersection a b)))
    (testing "2 is contained within 1" (is (sut/intersection b a))))
  (testing "non-overlapping intervals"
    (let [[a b] (intervals [0 1] [2 3])] (is (not (sut/intersection a b))))
    (let [[a b] (intervals [0 2] [2 3])] (is (not (sut/intersection a b))))))
