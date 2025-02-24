(ns sherlockbench.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [replicant.dom :as r]
            [reitit.frontend :as reitit]
            [reitit.frontend.easy :as reitit-easy]
            [reitit.coercion.spec :as rss]
            [sherlockbench.logic :as logic]
            [sherlockbench.ui :as ui]
            [sherlockbench.utility :refer [valid-uuid?]]
            [cljs.pprint :refer [pprint]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(defn redirect [path]
  (set! js/window.location.hash (str "#/" path)))

(defn valid-run?
  "does this id reference a valid run?"
  [run-id]
  (if (and
       (valid-uuid? run-id)
       (go (let [response (<! (http/post "http://localhost:3000/api/is-pending-run"
                                         {:with-credentials? false
                                          :json-params {:run-id run-id}}))]
             (tap> (:status response))
             (tap> (:body response)))))
    "boop")
  )

(defn root
  "this checks the state and redirects as appropriate"
  [{{{run-id :run-id} :query} :parameters :as match} store el]
  (cond
    ;; have we been provided with a valid run id?
    (valid-run? run-id) "boop"
    )
  (redirect (str "about?run-id=" run-id))
  )

(defn about-page [match store el]
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
