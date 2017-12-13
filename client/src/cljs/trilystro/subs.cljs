;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require    [clojure.spec.alpha :as s]
               [re-frame.core :as re-frame]
               [re-frame.loggers :refer [console]]
               [iron.re-utils :as re-utils :refer [sub2]]
               [trilystro.db :as db]))

;;; [TODO] Get rid of this file.

(sub2 :git-commit [:git-commit])
(sub2 ::db/name   [::db/name])

(re-frame/reg-sub
 :form-state
 (fn [db [_ form form-component]]
   (get-in db `[:forms ~form ~@form-component])))
