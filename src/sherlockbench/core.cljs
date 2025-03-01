(ns sherlockbench.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [replicant.dom :as r]
            [reitit.frontend :as reitit]
            [reitit.frontend.easy :as reitit-easy]
            [reitit.coercion.spec :as rss]
            [sherlockbench.logic :as logic]
            [sherlockbench.ui :as ui]
            [sherlockbench.utility :refer [valid-uuid?]]
            [sherlockbench.storage :as storage]
            [cljs.pprint :refer [pprint]]
            [cljs.core.async :refer [<!]]
            [clojure.string :as str]))

(def contact-us [:a {:href "mailto:joseph@xylon.me.uk"} "contact us"])

(def pass (constantly nil))

(defonce store (atom nil))
(defonce log-store (atom nil))

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
            [:h1 "Please Wait..."]
            [:p "If you see this for more than a few seconds something "
             "is broken. Please " contact-us]]
   :action-fn
   (fn []
     (if (not (valid-uuid? run-id))
       ;; there's no query string
       (reitit-easy/push-state :landing-anonymous {} {})

       ;; there is a query string with a uuid
       (let [run-data (storage/get-run run-id)]
         (if (nil? run-data)
           ;; we didn't start any run yet
           (go
             (if (<! (logic/valid-run? run-id))
               ;; it references a valid run
               (reitit-easy/push-state :landing-competition {:run-id run-id} {})
               (reitit-easy/push-state :error-run-id {:run-id run-id} {})))
           
           ;; we already started the run and have data for it
           (do
             ;; get the localstorage data into the atom
             (reset! store run-data)
             ;; redirect to index
             ;; TODO if run is complete they should see the results page
             (reitit-easy/push-state :index {:run-id run-id} {}))))))})

(defn error-run-id-page [{{:keys [run-id]} :path-params} _ _]
  {:hiccup
   [:div
    [:h1 "Invalid Run"]
    [:p "Either your run ID is invalid/expired, or this run is already "
     "in-progress in another browser."]
    [:p "If this is wrong please " contact-us]]
   :action-fn pass})

(defn landing-anonymous-page [_ _ _]
  {:hiccup
   [:div
    [:h1 "Take the SherlochBench test!"]
    [:p "Here you can take the SherlockBench test yourself."]
    [:p "The test is anonymous (this site doesn't use cookies) but we "
     "do record the results of the test in our system."]
    [:p "This is an example set of questions just to demonstrate how the test
   works. If you want to take the full test please " contact-us]
    [:button {:on {:click [[:action/start-run nil]]}
              :style {:margin-top 20
                      :font-size 20}}
     "Start Test"]]
   
   :action-fn pass})

(defn landing-competition-page [{{:keys [run-id]} :path-params} _ _]
  {:hiccup
   [:div
    [:h1 "Take the SherlochBench test!"]
    [:p "Here you can take the SherlockBench test."]
    [:p "The link you used allows you to take the test with the "
     "\"competition\" problem set, which is the same problem set we test the "
     "AIs with. However it also means the test is " [:em "not"] " anonymous."]
    [:p "We save the following information about your test:"]
    [:ul
     [:li "The time the test was started"]
     [:li "Which questions were answered right or wrong"]
     [:li "Your over-all score"]]
    [:p "If you wish to practice first, try the anonymous version "
     [:a {:href (reitit-easy/href :landing-anonymous)
          :target "_blank"} "here"] "."]
    [:p "Once you start you will have 24 hours to complete the test."]
    [:p "The problems are not ordered by difficulty. If you find one "
     "too hard, skip it and come back to it later."]
    [:button {:on {:click [[:action/start-run run-id]]}
              :style {:margin-top 20
                      :font-size 20}}
     "Start Test"]]

   :action-fn pass})

(defn collect-input-form-values []
  (loop [idx 0
         values []]
    (let [input-el (js/document.getElementById (str "input-" idx))]
      (if input-el
        (let [value (.-value input-el)
              form-type (-> input-el .-parentNode .-firstChild .-textContent
                           (str/split #"\(|\)") second)]
          (recur (inc idx)
                 (conj values 
                       (case form-type
                         "integer" (js/parseInt value)
                         "boolean" (= value "true")
                         value))))
        values))))

(defn restore-store [run-id store]
  (when (nil? @store)
    (let [run-data (storage/get-run run-id)]
      (reset! store run-data))))

(defn index-page [{{:keys [run-id]} :path-params} store _]
  (restore-store run-id store)
  {:hiccup (ui/render-index-page run-id @store)
   :action-fn pass})

(defn attempt-page [{{:keys [run-id attempt-id]} :path-params} store el]
  (restore-store run-id store)
  (let [attempt-log
        (storage/get-attempt attempt-id)

        render-fn
        (fn [run-data log]
          (let [attempts (:attempts run-data)
                attempt (logic/find-attempt-by-id attempts attempt-id)]

            (if attempt
              (ui/render-attempt-page run-id attempt log)
              [:div 
               [:h1 "Problem Not Found"]
               [:p "The requested problem could not be found."]
               [:p [:a {:href (reitit-easy/href :index {:run-id run-id})} "Return to Index"]]])))]

    ;; restore the log from localStorage, or initialize it
    (reset! log-store (or attempt-log []))

    {:hiccup
     (render-fn @store @log-store)

     :action-fn
     (fn []
       (add-watch log-store ::render-log
                  (fn [_ _ _ log]
                    (r/render el (render-fn @store log)))))}))

(defn main []
  (let [el (js/document.getElementById "app")]

    ;; Globally handle DOM events
    (r/set-dispatch!
     (fn [{:keys [replicant/dom-event]} data]
       ;; data will be a vector of vectors like:
       ;; [[:event args] [:event args]]
       ;; dom-event is handy for (.preventDefault dom-event)
       (doseq [[action & args] data]
         (prn action args)
         (case action
           :action/prevent-default
           (.preventDefault dom-event)

           :action/alert
           (js/alert (first args))

           :action/start-run
           (apply logic/start-run store args)

           :action/goto-page
           (apply reitit-easy/push-state args)
           
           :action/test-mystery-function
           (let [values (collect-input-form-values)]
             (prn "Testing mystery function with values:" values)
             (apply logic/test-function values log-store args))
           
           (prn "Unknown action:" data)))))

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
                                     :parameters {:path {:run-id string?}}}]
                  ["/attempt/:run-id/:attempt-id" {:name :attempt
                                                   :view attempt-page
                                                   :parameters {:path {:run-id string?
                                                                       :attempt-id string?}}}]]]

      ;; Initialize Reitit router
      (reitit-easy/start!
       (reitit/router routes {:data {:coercion rss/coercion}})
       (fn [match]
         (let [view-fn (get-in match [:data :view] (fn [_ _] {:hiccup [:div "Page not found"]
                                                              :action-fn pass}))
               {:keys [hiccup action-fn]} (view-fn match store el)]
           (r/render el hiccup)
           (action-fn)))
       {:use-fragment true}))))
