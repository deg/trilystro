(ns chromex-sample.popup.handlers
  (:require [re-frame.core :as re-frame]
            [chromex-sample.popup.db :as db]))

(re-frame/reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

