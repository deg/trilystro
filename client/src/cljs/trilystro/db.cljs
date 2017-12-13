;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.db
  (:require
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [re-frame.core :as re-frame :refer [after]]
   [re-frame.loggers :refer [console]]
   [trilib.db :as lib-db]))


(def check-spec-interceptor (after (partial lib-db/check-and-throw ::db)))


(def default-db
  (assoc lib-db/default-db
         ::name "Trilystro"))


(s/def ::name string?)

(s/def ::commit string?)
(s/def ::date string?)  ;; [TODO] Needs to be namespaced! Ditto for rest of git and user
(s/def ::git-commit (s/keys :req-un [::commit ::date]))

(s/def ::db (s/merge :trilib.db/db
                     (s/keys :req [::name] :opt-un [::git-commit])))
