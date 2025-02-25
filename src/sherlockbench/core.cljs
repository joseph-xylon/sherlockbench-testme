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
  "Asynchronously checks if a given run-id references a valid run, returns a channel."
  [run-id]
  (go
    (let [response (<! (http/post "http://localhost:3000/api/is-pending-run"
                                  {:with-credentials? false
                                   :json-params {:run-id run-id}}))]
      (:response (:body response)))))

(defn root
  "Checks the state and redirects as appropriate, asynchronously."
  [{{{run-id :run-id} :query} :parameters :as match} store el]
  (if (not (valid-uuid? run-id))
    ;; there's no query string
    (redirect (str "landing-anon"))

    ;; there is a query string with a uuid
    (let [storage-contents (.getItem js/localStorage run-id)]
      (if (not storage-contents)
        ;; we didn't start any run yet
        (go
          (if (<! (valid-run? run-id))
            ;; it references a valid run
            (redirect (str "landing-competition?run-id=" run-id))
            (redirect (str "error?run-id=" run-id))))
        
        ;; we already started the run and have data for it
        (do
          ;; get the localstorage data into the atom
          (reset! store storage-contents)
          (redirect (str "index?run-id=" run-id))))))

  ;; reitit won't like the channel so we give it some hiccup :)
  [:div
   [:h1 "Routing"]])

(defn about-page [match store el]
  [:div
   [:h1 "About SherlockBench"]
   [:p "This is an app built with Replicant and Reitit."]])

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
             (r/render el
                       (view-fn match store el)))))
       {:use-fragment true}))))
