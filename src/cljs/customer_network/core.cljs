(ns customer-network.core
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [reagent.core :as r :refer [atom]]
              [cljs-http.client :as http]
              [cljs.core.async :refer [<! >! chan put!]]
              [customer-network.components :refer [d3-container]]))

;; -------------------------
;; "Container" component (responsible for mediating all network interaction/state management
;;  and handing data down to "pure" components for rendering)

(defn customer-network-index []
  (let [graph-data (r/atom {:nodes [] :links []})
        handle-graph-data-load (fn []
          (go (let [response (<! (http/get "/api/d3-data" {:with-credentials? false}))]
              (reset! graph-data (:body response)))))
        handle-vertex-click (fn [id]
          (go (<! (http/put (str "/api/vertices/" id) {:json-params {:fraudulent true}}))
                  (handle-graph-data-load)))]
    (handle-graph-data-load)
    (fn []
      [:div {:class "container"}
        [:h1 {:class "hero-title"} "Customer Network"]
          [:h4 {:class "hero-subtitle"} "Click a vertex (with radius representing score) to mark it as fraudulent and see the effects through the network"]
          [d3-container {:data (clj->js @graph-data) :on-vertex-click handle-vertex-click}]])))


;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [customer-network-index] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
