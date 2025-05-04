(ns sherlockbench.ui
  (:require [reitit.frontend.easy :as reitit-easy]
            [clojure.string :as str]
            [cljs.pprint :refer [pprint]]))

;; This file contains pure functions which take some state and return hiccup
;; https://replicant.fun/hiccup/
;;
;; There are two special functions:
;; - the top-level function, which everything else is a child of. This is
;;   re-rendered when our atom changes
;; - optionally a data transformation function. this just adapts
;;   the "business domain data" to the right format for the UI

;; cute: "✓ Correct" "✗ Incorrect"

(defn render-attempt-link [{:keys [attempt-id
                                   problem-name
                                   arg-spec
                                   state] :as attempt} run-id]
  [:li {:key attempt-id}
   [:a (when (#{:investigate :verify} state)
         {:href "#"
          :on {:click [[:action/prevent-default]
                       [:action/goto-page :attempt {:run-id run-id :attempt-id attempt-id}]]}})
    [:span problem-name]
    (case state
      :completed
      [:span {:style {:color "green"
                      :font-weight "bold"}}
       " ✓ Completed"]

      :abandoned
      [:span {:style {:color "darkred"
                      :font-weight "bold"}}
       " ⊗ Abandoned"]
      nil)]])

(defn render-attempts-list [run-id attempts]
  [:div
   [:h3 "Problems:"]
   [:ul {:style {:padding "0"}}
    (map #(render-attempt-link % run-id) attempts)]])

(defn render-index-page
  [run-id {:keys [attempts run-type]}]
  [:div.landing
   [:h2 "SherlockBench Test"]
   (when (seq attempts)
     (render-attempts-list run-id attempts))

   [:div {:style {:margin-top "20px"}}
    [:p "Complete all problems to finish the test. You can work on problems in any order."]]
   ])

(defn render-result-link [{:keys [attempt-id
                                   problem-name
                                   function_name
                                   result] :as attempt} run-id]
  [:tr {:key attempt-id}
   [:td problem-name]
   [:td function_name]
   (case result
     :correct
     [:td {:style {:color "green"
                   :font-weight "bold"}}
      " ✓ Correct"]

     :wrong
     [:td {:style {:color "darkred"
                   :font-weight "bold"}}
      " ⊗ Wrong"]

     :abandoned
     [:td {:style {:color "darkred"
                   :font-weight "bold"}}
      " ⊗ Abandoned"])])

(defn render-results-list [run-id attempts]
  [:div
   [:h3 "Breakdown:"]
   [:table
    [:thead
     [:tr
      [:th "Problem"]
      [:th "Description"]
      [:th "Result"]]]
    [:tbody
     (map #(render-result-link % run-id) attempts)]]])

(defn render-results-page
  [run-id {:keys [final-score attempts run-type] :as run-data}]
  [:div
   [:h2 "Results"]
   [:p (str "Your over-all score: " (:numerator final-score) "/" (:denominator final-score))]

   (render-results-list run-id attempts)]
  )

(defn render-input-field [arg-type idx]
  (let [id (str "input-" idx)
        human-type (case arg-type
                     "string" "text"
                     ("integer" "float") "number"
                     "boolean" "true or false"
                     "value")
        input-type (case arg-type
                     "integer" "number"
                     "text")]
    [:div.form-group
     [:label {:for id} (str "Input " (inc idx) " (" human-type "):")]
     (if (= arg-type "boolean")
       [:select {:id id
                 :name id}
        [:option {:value "true"} "true"]
        [:option {:value "false"} "false"]]
       [:input {:type input-type
                :id id
                :name id
                :placeholder (str "Enter " human-type)
                :autocapitalize "off"
                :autocomplete "off"
                :autocorrect "off"
                :spellcheck "false"}])]))

(defn investigation-input-form [run-id attempt-id arg-spec]
  [:form.attempt-form
   [:h3 "Test the Mystery Function"]
   (map-indexed
    #(render-input-field %2 %1)
    arg-spec)
   
   [:button.submit-btn {:on {:click [[:action/prevent-default]
                                     [:action/test-mystery-function run-id attempt-id]]}} "Submit"]])

(defn verification-input-form [run-id attempt-id {{:keys [inputs output-type]} :next-verification}]
  (let [input-type (case output-type
                     "integer" "number"
                     "text")]
    [:form.attempt-form
     [:h3 "What will the output be with these inputs?"]
     [:div.form-group
      [:label {:for "expected-out"} (str/join ", " inputs)]
      (if (= output-type "boolean")
        [:select#expected-out
         [:option {:value "true"} "true"]
         [:option {:value "false"} "false"]]
        [:input#expected-out {:type input-type
                              :autocapitalize "off"
                              :autocomplete "off"
                              :autocorrect "off"
                              :spellcheck "false"}])]
     
     [:button.submit-btn {:on {:click [[:action/prevent-default]
                                       [:action/attempt-verification run-id attempt-id]]}} "Submit"]]))

(defn scroll-log-container []
  (when-let [container (js/document.querySelector ".log-container")]
    (let [scroll-options (js-obj "top" (.-scrollHeight container) "behavior" "smooth")]
      (.scrollTo container scroll-options))))

(defn render-log-content [log]
  [:div.log-container
   (seq log)])

(defn control-buttons [run-id attempt-id state]
  (list
   [:button.control {:on {:click [[:action/goto-page :index {:run-id run-id}]]}} "← Back to problem list"]

   (when (= state :investigate)
     [:button.control {:on {:click [[:action/get-verification run-id attempt-id]]}} "I'm Ready"])

   (case state
     (:investigate :verify)
     [:button#abandon.control {:on {:click [[:action/prompt-abandon run-id attempt-id]]}} "Abandon"]

     (:completed :abandoned)
     [:button#continue.control {:on {:click [[:action/goto-next-problem run-id attempt-id]]}} "Continue"])))

(comment
  ;; example attempt map
  {:attempt-id "cbb0a1dc-5d1f-4721-942e-d0b6e5ae36df",
   :arg-spec ["integer" "integer" "integer"]
   :problem-name "Problem 1" 
   :state :investigate ; :verify :completed :abandoned
   }

  ;; state values: :investigate :verify :completed :abandoned

  ;; example attempt-data
  {:log []
   :next-verification {:inputs [2 5]
                       :output-type "integer"}}  ; "string" "boolean" "float"
  )

(defn render-attempt-page
  [run-id
   {:keys [attempt-id arg-spec problem-name test-limit state] :as attempt}
   {:keys [log] :as attempt-data}]

  [:div.attempt-page
   [:h2 problem-name]
   (case state
     :investigate
     (list
      [:p "Test the mystery function until you think you know what it does. Then
   click \"I'm Ready\" and the system will test you."]
      [:p (str "You may test this function up-to " test-limit " times.")])

     :verify
     [:p "Prove you have figured out what the function does."]

     :completed
     [:p "You have completed this problem."]

     :abandoned
     [:p "You have abandoned this problem."])

   [:div.attempt-container
    ;; Input section
    [:div.attempt-input-section
     (case state
       :investigate
       (investigation-input-form run-id attempt-id arg-spec)

       :verify
       (verification-input-form run-id attempt-id attempt-data)

       `())]
    
    ;; Log section
    [:div.attempt-log-section
     [:h3 "Test Log"]
     (render-log-content log)]]
   
   [:div.attempt-navigation
    (control-buttons run-id attempt-id state)]])
