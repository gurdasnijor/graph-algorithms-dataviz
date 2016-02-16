(ns customer-network.handler
  (:require [compojure.core :refer [GET PUT POST context defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [api-defaults site-defaults wrap-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response content-type]]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-js include-css]]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.reload :refer [wrap-reload]]
            [environ.core :refer [env]]
            [customer-network.core :refer [load-edges score-map]]))

;In-memory representations of edges + fraudulent customer ids
(defonce edges (atom (load-edges "resources/edges.txt")))
(defonce fraudulent-customers (atom #{}))

(def loading-page
  (html
   [:html
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     (include-css (if (env :dev) "css/site.css" "css/site.min.css"))]
    [:body
     [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]]
     (include-js "js/app.js")]]))

(defn vertex-list [sort]
  (->> (score-map @edges @fraudulent-customers)
       (sort-by last (if (= sort "asc") > <))
       (map (fn [[id score]]
              {:id id :score score :fraudulent (contains? @fraudulent-customers id)}))))

(defn create-edge [{edge :body}]
  (let [new-edge (map (comp keyword str) edge)]
    (swap! edges #(conj % new-edge)))
  {:status 201})

(defn get-edges [req]
  (-> @edges
      response
      (content-type "application/json")))

(defn get-vertices [req]
  (let [sort-param (get (:params req) :sort)]
    (-> (vertex-list sort-param)
        response
        (content-type "application/json"))))

(defn update-vertex [req]
  (let [id (keyword (get (:params req) :id))
        body (get-in req [:body])]
    (if (-> body vals first)
      (swap! fraudulent-customers #(conj % id))
      (swap! fraudulent-customers #(disj % id)))
    {:status 200}))

;This route formats edge and vertex data in a way that can be directly consumed
;by a d3 force directed layout
(defn get-force-layout-data [req]
  (let [vertex-index-map (into (sorted-map) (map-indexed (fn [idx itm] {itm idx}) (sort (distinct (apply concat @edges)))))
        closeness-map (score-map @edges @fraudulent-customers)
        links (map (fn [[source target]] {:source (get vertex-index-map source) :target (get vertex-index-map target)}) @edges)
        nodes (map (fn [[k v]] {:name k :size (get closeness-map k)}) vertex-index-map)]
    (-> {:nodes nodes :links links}
        response
        (content-type "application/json"))))

(defroutes routes
  (GET "/" [] loading-page)
  (resources "/")
  (context "/api" []
    (GET "/edges" [] get-edges)
    (POST "/edges" [] create-edge)
    (GET "/vertices" [] get-vertices)
    (PUT "/vertices/:id" [] update-vertex)
    (GET "/d3-data" [] get-force-layout-data))
  (not-found "Not Found"))

(def app
  (let [handler (-> (wrap-defaults #'routes (assoc-in api-defaults [:security :anti-forgery] false))
                    wrap-json-response
                    wrap-json-body)]
    (if (env :dev) (-> handler wrap-exceptions wrap-reload) handler)))
