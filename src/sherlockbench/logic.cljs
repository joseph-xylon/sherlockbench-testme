(ns sherlockbench.logic
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.pprint :refer [pprint]]
            [sherlockbench.storage :as storage]
            [cljs.core.async :refer [<!]]
            [reitit.frontend.easy :as reitit-easy]
            [clojure.string :as str])
  )

;; Functions in this file will be called from core.cljs.
;; Usually from the global event handler: https://replicant.fun/event-handlers/

(defn process-attempts 
  "Processes attempt data by adding problem name and initial state"
  [attempts]
  (map #(assoc %1 
               :problem-name (str "Problem " %2)
               :state :investigate)
       attempts (range 1 js/Infinity)))

(defn valid-run?
  "Asynchronously checks if a given run-id references a valid run, returns a channel."
  [run-id]
  (go
    (let [response (<! (http/post "http://localhost:3000/api/is-pending-run"
                                  {:with-credentials? false
                                   :json-params {:run-id run-id}}))]
      (:response (:body response)))))

(defn start-run [store run-id]
  (go
    (let [response (<! (http/post "http://localhost:3000/api/start-run"
                                  {:with-credentials? false
                                   :json-params (cond-> {:client-id "sherlockbench-testme"
                                                         :subset :easy3}
                                                  (not (nil? run-id)) (assoc :existing-run-id run-id))}))
          {{:keys [run-id run-type benchmark-version attempts]} :body} response
          attempts-named (process-attempts attempts)
          run-data {:run-id run-id
                    :run-type run-type
                    :benchmark-version benchmark-version
                    :attempts attempts-named}]

      (prn (str "Starting " run-type " benchmark with run-id: " run-id))

      ;; update the atom
      (reset! store run-data)
      ;; update the localStorage
      (storage/set-run! run-id run-data)
      ;; redirect to the index page
      (reitit-easy/push-state :index {:run-id run-id} {})
      )))

(defn test-function [values attempt-store run-id attempt-id]
  (go
    (let [response (<! (http/post "http://localhost:3000/api/test-function"
                                  {:with-credentials? false
                                   :json-params {:run-id run-id
                                                 :attempt-id attempt-id
                                                 :args values}}))
          {{output :output} :body} response]
      
      (swap! attempt-store update :log conj [:p (str (str/join ", " values) " → " output)])
      (storage/set-attempt! attempt-id @attempt-store)
      )))

(defn find-attempt-by-id [attempts attempt-id]
  (first (filter #(= (:attempt-id %) attempt-id) attempts)))

(defn find-next-problem 
  "Finds the next problem that isn't completed or abandoned"
  [attempts current-attempt-id]
  (let [current-index (->> attempts
                           (map-indexed vector)
                           (filter #(= (:attempt-id (second %)) current-attempt-id))
                           first
                           first)
        remaining-attempts (->> attempts
                                (drop (inc current-index))
                                (filter #(not (#{:completed :abandoned} (:state %)))))]
    (first remaining-attempts)))

(defn update-attempt-by-id 
  "Updates an attempt with the given attempt-id in the store, setting the specified keys to the provided values.
   Returns the updated store value.
   
   Can be called with a single key-value pair:
   Example: (update-attempt-by-id store attempt-id :state :verify)
   Example: (update-attempt-by-id store attempt-id :completed true)
   
   Or with multiple key-value pairs:
   Example: (update-attempt-by-id store attempt-id :state :verify :completed true)
   Example: (update-attempt-by-id store attempt-id :result \"correct\" :state :completed)"
  [store attempt-id & kvs]
  (when (odd? (count kvs))
    (throw (js/Error. "update-attempt-by-id requires an even number of key-value arguments")))
  
  (swap! store update :attempts 
         (fn [attempts]
           (map (fn [attempt]
                  (if (= (:attempt-id attempt) attempt-id)
                    (apply assoc attempt kvs)
                    attempt))
                attempts))))

(defn get-verification [store attempt-store run-id attempt-id & [message]]
  (go
    (let [response (<! (http/post "http://localhost:3000/api/next-verification"
                                  {:with-credentials? false
                                   :json-params {:run-id run-id
                                                 :attempt-id attempt-id}}))
          {{:keys [next-verification output-type]} :body} response]

      ;; update the atoms
      (update-attempt-by-id store attempt-id :state :verify)
      
      (swap! attempt-store #(-> %
                             (assoc :next-verification {:inputs next-verification
                                                        :output-type output-type})
                             (update :log conj (or message [:h3 "Verifications"]))))

      ;; save to localstorage
      (storage/set-attempt! attempt-id @attempt-store)
      (storage/set-run! run-id @store)
      )))

(defn attempt-verification [store attempt-store value run-id attempt-id]
  (go
    (let [response (<! (http/post "http://localhost:3000/api/attempt-verification"
                                  {:with-credentials? false
                                   :json-params {:run-id run-id
                                                 :attempt-id attempt-id
                                                 :prediction value}}))
          {{:keys [status]} :body} response]  ; correct/done/wrong

      (case status
        ("wrong" "done")
        (do
          ;; set status to completed and record result as right or wrong
          (update-attempt-by-id store attempt-id 
                               :state :completed
                               :result ({"done" :correct
                                         "wrong" :wrong} status))

          ;; write to log
          (swap! attempt-store update :log conj [:p "Verifications complete"])

          ;; save to localstorage
          (storage/set-run! run-id @store)
          (storage/set-attempt! attempt-id @attempt-store)
          )

        "correct"
        (get-verification store attempt-store run-id attempt-id [:p "Verification successful. Next verification..."]))))

  )

(defn add-function-names
  "warning: Claude wrote this and the tests too and I haven't groked it"
  [attempts problem-names]
  (let [id-to-function-name (reduce (fn [acc {:keys [id function_name]}]
                                      (assoc acc id function_name))
                                    {}
                                    problem-names)]
    (map (fn [attempt]
           (if-let [function-name (get id-to-function-name (:attempt-id attempt))]
             (assoc attempt :function_name function-name)
             attempt))
         attempts)))

(defn submit-run 
  "Submits the completed run to the server and returns a channel that
   completes when the submission is done"
  [store run-id]
  (go
    (let [attempts (:attempts @store)]
      (let [response
            (<! (http/post "http://localhost:3000/api/complete-run"
                           {:with-credentials? false
                            :json-params {:run-id run-id}}))

            {{:keys [score problem-names]} :body} response]
        
        (swap! store
               #(-> %
                    (assoc :run-state :submitted
                           :final-score score
                           :attempts (add-function-names attempts problem-names))))

        ;; save to localstorage
        (storage/set-run! run-id @store)
        
        ;; Return true to signal completion
        true))))
