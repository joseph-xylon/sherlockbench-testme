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

(def contact-us [:a {:href "mailto:joseph@xylon.me.uk"} "contact us"])

(def pass (constantly nil))

(defonce store (atom nil))

(defn valid-run?
  "Asynchronously checks if a given run-id references a valid run, returns a channel."
  [run-id]
  (go
    (let [response (<! (http/post "http://localhost:3000/api/is-pending-run"
                                  {:with-credentials? false
                                   :json-params {:run-id run-id}}))]
      (:response (:body response)))))

;; In this application, view functions follow a specific pattern to properly
;; separate rendering from side effects (like routing):
;;
;; 1. Each view function returns a map with two keys:
;;    - :hiccup    - The UI to render (pure, no side effects)
;;    - :action-fn - Function to execute after rendering (for side effects)
;;
;; 2. The router renders the :hiccup content first, then calls the :action-fn
;;    afterwards, ensuring that side effects happen after DOM updates.
;;
;; This pattern solves common issues with single-page applications:
;; - Avoids race conditions between rendering and navigation
;; - Creates predictable timing for side effects
;; - Keeps rendering functions pure (React-like philosophy)
;; - Makes the code more maintainable and easier to reason about

(defn root
  "Checks the state and redirects as appropriate, asynchronously."
  [{{{run-id :run-id} :query} :parameters :as match} store]
  {:hiccup [:div
            [:h1 "Routing"]]
   :action-fn
   (fn []
     (if (not (valid-uuid? run-id))
       ;; there's no query string
       (reitit-easy/push-state :landing-anonymous {} {})

       ;; there is a query string with a uuid
       (let [storage-contents (.getItem js/localStorage run-id)]
         (if (not storage-contents)
           ;; we didn't start any run yet
           (go
             (if (<! (valid-run? run-id))
               ;; it references a valid run
               (reitit-easy/push-state :landing-competition {:run-id run-id} {})
               (reitit-easy/push-state :error-run-id {:run-id run-id} {})))
           
           ;; we already started the run and have data for it
           (do
             ;; get the localstorage data into the atom
             (reset! store storage-contents)
             (reitit-easy/push-state :index {:run-id run-id} {}))))))}

  ;; reitit won't like the channel so we give it some hiccup
  )

(defn error-run-id-page [{{:keys [run-id]} :path-params} _]
  {:hiccup
   [:div
    [:h1 "Invalid Run"]
    [:p "Either your run ID is invalid/expired, or this run is already in-progress in another browser."]
    [:p "If this is wrong please " contact-us]]
   :action-fn pass})

(defn landing-anonymous-page [_ _]
  {:hiccup
   [:div
    [:h1 "Take the SherlochBench test!"]
    [:p "Here you can take the SherlockBench test yourself."]
    [:p "This is an example set of questions just to demonstrate how the test
   works. If you want to take the full test please " contact-us]
    [:button {:on {:click [:start-run-anon]}
              :style {:margin-top 20
                      :font-size 20}}
     "Start Test"]]
   
   :action-fn pass})

(defn landing-competition-page [{{:keys [run-id]} :path-params} _]
  {:hiccup
   [:div
    [:h1 "Take the SherlochBench test!"]
    [:p "Here you can take the SherlockBench test yourself."]]

   :action-fn pass})

(defn index-page [{{:keys [run-id]} :path-params} _]
  {:hiccup
   [:div
    [:h1 "SherlockBench Test"]
    [:p "Your test is ready. Run ID: " run-id]]
   :action-fn pass})

(defn start-run [store run-id]
  (go
    (let [response (<! (http/post "http://localhost:3000/api/start-run"
                                  {:with-credentials? false
                                   :json-params (cond-> {:client-id "sherlockbench-testme"}
                                                  (not (nil? run-id)) (assoc :existing-run-id run-id))}))
          {{:keys [run-id run-type benchmark-version attempts]} :body} response]
      (pprint response)
      (prn (str "Starting " run-type " benchmark with run-id: " run-id))
      
      (reset! store {:run-id run-id
                     :run-type run-type
                     :benchmark-version benchmark-version
                     :attempts attempts})

      )))

(defn main []
  (let [el (js/document.getElementById "app")]

    ;; Globally handle DOM events
    (r/set-dispatch!
     (fn [_ [action & args]]
       (case action
         :boop (apply swap! store logic/boop args)
         :start-run-anon (start-run store nil))))

    ;; Define routes
    (let [routes [["/" {:name :home
                        :view root}]
                  ["/error-run-id/:run-id" {:name :error-run-id
                                            :view error-run-id-page
                                            :parameters {:path {:run-id string?}}}]
                  ["/landing-anonymous" {:name :landing-anonymous
                                         :view landing-anonymous-page}]
                  ["/landing-competition/:run-id" {:name :landing-competition
                                                  :view landing-competition-page
                                                  :parameters {:path {:run-id string?}}}]
                  ["/index/:run-id" {:name :index
                                    :view index-page
                                    :parameters {:path {:run-id string?}}}]]]

      ;; Initialize Reitit router
      (reitit-easy/start!
       (reitit/router routes {:data {:coercion rss/coercion}})
       (fn [match]
         (let [view-fn (get-in match [:data :view] (fn [_ _] {:hiccup [:div "Page not found"]
                                                              :action-fn pass}))
               {:keys [hiccup action-fn]} (view-fn match store)]
           (r/render el hiccup)
           (action-fn)))
       {:use-fragment true}))))
