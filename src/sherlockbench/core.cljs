(ns sherlockbench.core
  (:require [replicant.dom :as r]
            [reitit.frontend :as reitit]
            [reitit.frontend.easy :as reitit-easy]
            [reitit.coercion.spec :as rss]
            [sherlockbench.logic :as logic]
            [sherlockbench.ui :as ui]
            [cljs.pprint :refer [pprint]]))

(defn redirect [path]
  (set! js/window.location.hash (str "#/" path)))

(defn root
  "this checks the state and redirects as appropriate"
  [{{{run-id :run-id} :query} :parameters :as match} store el]
  (redirect (str "about?run-id=" run-id))
  )

(defn about-page [match store el]
  (pprint match)
  (r/render el [:div
                [:h1 "About SherlochBench"]
                [:p "This is an app built with Replicant and Reitit."]]))

(defn main []
  (let [store (atom nil)
        el (js/document.getElementById "app")]

    ;; Globally handle DOM events
    (r/set-dispatch!
     (fn [_ [action & args]]
       (case action
         :boop (apply swap! store logic/boop args))))

    ;; Define routes
    (let [routes [["/" {:name :home
                        :view root}]
                  ["/about" {:name :about
                             :view about-page
                             :parameters {:query {:run-id string?}}}]]]

      ;; Initialize Reitit router
      (reitit-easy/start!
       (reitit/router routes {:data {:coercion rss/coercion}})
       (fn [match]
         (let [view-fn (get-in match [:data :view])]
           (when view-fn
             (view-fn match store el))))
       {:use-fragment true}))))
