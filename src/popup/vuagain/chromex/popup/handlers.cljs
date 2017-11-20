(ns vuagain.chromex.popup.handlers
  (:require [re-frame.core :as re-frame]
            [vuagain.chromex.popup.db :as db]))

(re-frame/reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

