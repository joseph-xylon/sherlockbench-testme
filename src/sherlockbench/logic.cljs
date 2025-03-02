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
                                   :json-params (cond-> {:client-id "sherlockbench-testme"}
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

(defn update-attempt-by-id 
  "Updates an attempt with the given attempt-id in the store, setting the specified key to the provided value.
   Returns the updated store value.
   
   Example: (update-attempt-by-id store attempt-id :state :verify)
   Example: (update-attempt-by-id store attempt-id :completed true)"
  [store attempt-id key value]
  (swap! store update :attempts 
         (fn [attempts]
           (map (fn [attempt]
                  (if (= (:attempt-id attempt) attempt-id)
                    (assoc attempt key value)
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
          ;; set status to completed
          (update-attempt-by-id store attempt-id :state :completed)

          ;; record result as right or wrong
          (update-attempt-by-id store attempt-id :result {{"done" "correct"
                                                           "wrong" "wrong"} status})

          ;; write to log
          (swap! attempt-store update :log conj [:p "Verifications complete"])

          ;; save to localstorage
          (storage/set-run! run-id @store)
          (storage/set-attempt! attempt-id @attempt-store)
          )

        "correct"
        (get-verification store attempt-store run-id attempt-id [:p "Verification successful. Next verification..."]))))

  )

(defn if-run-complete [store run-id]
  (let [attempts (:attempts @store)]
    (when (every? #(= true (:completed %)) attempts)
      (let [response (<! (http/post "http://localhost:3000/api/complete-run"
                                    {:with-credentials? false
                                     :json-params {:run-id run-id}}))]
        true ;; todo finish this
        ))))
