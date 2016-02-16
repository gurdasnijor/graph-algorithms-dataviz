(ns customer-network.components
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [reagent.core :as r :refer [atom]]
              [reagent.session :as session]
              [cljsjs.d3 :as d3]))


(defn- build-links [svg graph]
  (let [links-selection (.. svg (selectAll ".link") (data (.-links graph)))]
    (.. links-selection
        enter
        (append "line")
        (attr "class" "link")
        (attr "stroke" "grey")
        (style "stroke-width" 1))
    (.. links-selection
        exit
        remove)
        links-selection))

(defn- build-nodes [svg graph force-layout handle-vertex-click color-scale]
  (let [nodes-selection (.. svg (selectAll ".node") (data (.-nodes graph)) )
        enter-selection (.. nodes-selection enter (append "g"))]
    (.. nodes-selection (selectAll "*") remove)
    (.. enter-selection
        (classed "node" true))
    (.. nodes-selection
        (append "circle")
        (attr "r" #(* 2000 (.-size %)))
        (style "fill" #(color-scale (.-size %)))
        (on "click" #(handle-vertex-click (.-name %)))
        (call (.-drag force-layout)))
    (.. nodes-selection
        (append "text")
        (attr "x" 30)
        (attr "dy" ".35em")
        (text #(str (.-name %))))
    nodes-selection ))

(defn- update-node [node]
  (.. node
      (attr "transform" #(str "translate(" (.. % -x) "," (.. % -y) ")"))))

(defn- update-link [link]
  (.. link
      (attr "x1" #(.. % -source -x))
      (attr "y1" #(.. % -source -y))
      (attr "x2" #(.. % -target -x))
      (attr "y2" #(.. % -target -y))))

(defn- on-tick [link node]
  (fn []
    (update-node node)
    (update-link link)))

(defn- build-force-layout []
  (.. js/d3
      -layout
      force
      (size (clj->js [800 800]))
      (linkDistance 400)
      (gravity 0.25)
      (charge -1000)))

(defn- build-color-scale []
  (.. js/d3
      -scale
      (category10)))

(defn- populate-force-layout [force-layout graph]
  (.. force-layout
      (nodes (.-nodes graph))
      (links (.-links graph))
      start))

(defn- sync-d3 [svg-selection force-layout color-scale handle-vertex-click data]
  (populate-force-layout force-layout data)
    (let [links (build-links svg-selection data)
          nodes (build-nodes svg-selection data force-layout handle-vertex-click color-scale)]
            (.on force-layout "tick" (on-tick links nodes))))

(defn d3-container [data]
  (let [force-layout (build-force-layout)
        color-scale (build-color-scale)]
    (r/create-class
      {:component-did-mount (fn [this]
        (let [data (-> this r/props :data)
              handle-vertex-click (-> this r/props :on-vertex-click)
              svg-selection (.select js/d3 (r/dom-node this))]
                (sync-d3 svg-selection force-layout color-scale handle-vertex-click data)))
       :should-component-update (fn [this old-argv new-argv]
         (let [[_ {data :data}] new-argv
               handle-vertex-click (-> this r/props :on-vertex-click)
               svg-selection (.select js/d3 (r/dom-node this))]
                (sync-d3 svg-selection force-layout color-scale handle-vertex-click data)))
       :display-name "data-viz"
       :reagent-render (fn [data]
         [:svg { :width 800 :height 800}])})))
