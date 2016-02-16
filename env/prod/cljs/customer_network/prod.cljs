(ns customer-network.prod
  (:require [customer-network.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
