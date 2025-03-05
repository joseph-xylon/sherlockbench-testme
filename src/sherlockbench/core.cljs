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
            [sherlockbench.forms :as forms]
            [cljs.pprint :refer [pprint]]
            [cljs.core.async :refer [<!]]
            [clojure.string :as str]))

(def contact-us [:a {:href "mailto:joseph@xylon.me.uk"} "contact us"])

(def pass (constantly nil))

(defonce store (atom nil))
(defonce attempt-store (atom nil))

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

(defn redirect-placeholder [s]
  [:div
   [:h1 s]
   [:p "If you see this for more than a few seconds something "
    "is broken. Please " contact-us]])

(defn root
  "Checks the state and redirects as appropriate, asynchronously."
  [{{{run-id :run-id} :query} :parameters :as match} store]
  {:hiccup (redirect-placeholder "Please Wait...")
   
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
    [:p "You may use a calculator if it helps."]
    [:button {:on {:click [[:action/start-run run-id]]}
              :style {:margin-top 20
                      :font-size 20}}
     "Start Test"]]

   :action-fn pass})


(defn restore-store [run-id store]
  (when (nil? @store)
    (let [run-data (storage/get-run run-id)]
      (reset! store run-data))))

(defn index-page [{{:keys [run-id]} :path-params} store _]
  (restore-store run-id store)

  (let [results-redirect
        (fn [] (reitit-easy/push-state :results {:run-id run-id} {}))]

    (if (= :submitted (:run-state @store))
      {:hiccup
       (redirect-placeholder "Redirecting to Results...")
       
       :action-fn
       results-redirect}

      (let [attempts (:attempts @store)]
        (if (every? (fn [x] (#{:completed :abandoned} (:state x))) attempts)
          ;; submit the run
          {:hiccup
           (redirect-placeholder "Test Complete. Submitting...")
       
           :action-fn
           (fn []
             (logic/submit-run store run-id)
             (results-redirect))}

          ;; render page normally
          {:hiccup
           (ui/render-index-page run-id @store)

           :action-fn
           pass})))))

(defn results-page [{{:keys [run-id]} :path-params} store _]
  {:hiccup
   (ui/render-results-page run-id @store)

   :action-fn
   pass})

(defn attempt-page [{{:keys [run-id attempt-id]} :path-params} store el]
  (restore-store run-id store)
  (let [attempt-data
        (storage/get-attempt attempt-id)

        render-fn
        (fn [attempt-data]
          (let [attempts (:attempts @store)
                attempt (logic/find-attempt-by-id attempts attempt-id)]
            (if attempt
              (ui/render-attempt-page run-id attempt attempt-data)
              [:div 
               [:h1 "Problem Not Found"]
               [:p "The requested problem could not be found."]
               [:p [:a {:href (reitit-easy/href :index {:run-id run-id})} "Return to Index"]]])))]

    ;; restore the attempt data from localStorage, or initialize it with empty log
    (reset! attempt-store (or attempt-data {:log []}))

    {:hiccup
     (render-fn @attempt-store)

     :action-fn
     (fn []
       (add-watch attempt-store ::render-attempt
                  (fn [_ _ _ attempt-data]
                    (r/render el (render-fn attempt-data))
                    ;; Scroll log container to bottom after rendering
                    (js/setTimeout ui/scroll-log-container 10))))}))

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
           (do
             (remove-watch attempt-store ::render-attempt)
             (apply reitit-easy/push-state args))
           
           :action/prompt-abandon
           (if (.confirm js/window "Are you sure you want to abandon this problem?")
             (let [[run-id attempt-id] args]
               (logic/update-attempt-by-id store attempt-id :state :abandoned)
               (storage/set-run! run-id @store))
             )

           :action/goto-next-problem
           (let [[run-id current-attempt-id] args
                 attempts (:attempts @store)
                 next-problem (logic/find-next-problem attempts current-attempt-id)]
             (if next-problem
               ;; If there's a next problem, navigate to it
               (do
                 (remove-watch attempt-store ::render-attempt)
                 (reitit-easy/push-state :attempt {:run-id run-id :attempt-id (:attempt-id next-problem)} {}))
               ;; Otherwise, go back to index
               (do
                 (remove-watch attempt-store ::render-attempt)
                 (reitit-easy/push-state :index {:run-id run-id} {}))))

           :action/test-mystery-function
           (let [values (forms/collect-input-form-values)]
             (prn "Testing mystery function with values:" values)
             (apply logic/test-function values attempt-store args)
             (forms/clear-input-form (count values)))

           :action/get-verification
           (apply logic/get-verification store attempt-store args)

           :action/attempt-verification
           (let [value (forms/collect-verification-form-value)]
             (apply logic/attempt-verification store attempt-store value args)
             (forms/clear-verification-form))
           
           (prn "Unknown action:" data)))))

    ;; Define routes
    (let [routes [["/"
                   {:name :home
                    :view root}]
                  
                  ["/error-run-id/:run-id"
                   {:name :error-run-id
                    :view error-run-id-page
                    :parameters {:path {:run-id string?}}}]
                  
                  ["/landing-anonymous"
                   {:name :landing-anonymous
                    :view landing-anonymous-page}]
                  
                  ["/landing-competition/:run-id"
                   {:name :landing-competition
                    :view landing-competition-page
                    :parameters {:path {:run-id string?}}}]
                  
                  ["/index/:run-id"
                   {:name :index
                    :view index-page
                    :parameters {:path {:run-id string?}}}]

                  ["/results/:run-id"
                   {:name :results
                    :view results-page
                    :parameters {:path {:run-id string?}}}]
                  
                  ["/attempt/:run-id/:attempt-id"
                   {:name :attempt
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
