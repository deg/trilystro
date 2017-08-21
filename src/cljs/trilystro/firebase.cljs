;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.firebase
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [reagent.ratom :as rv]
   [sodium.utils :as utils]
   [sodium.chrome-utils :as chrome]
   [sodium.re-utils :refer [<sub >evt]]))


;;; Built on ideas and code from
;;; http://timothypratley.blogspot.co.il/2016/07/reacting-to-changes-with-firebase-and.html
;;; and https://github.com/timothypratley/voterx

;;; TODO
;;; - re-frame'ify read and write

;;; This files assumes the following Firebase auth rules:
;;; // These rules grant access to a node matching the authenticated
;;; // user's ID from the Firebase auth token, plus a public area.
;;; {
;;;   "rules": {
;;;     "public": {".read": true, ".write": true},
;;;     "users": {
;;;       "$uid": {
;;;         ".read": "$uid === auth.uid",
;;;         ".write": "$uid === auth.uid"
;;;       }
;;;     }
;;;   }
;;;  }


(defn- fb-ref [path]
  (.ref (js/firebase.database) (str/join "/" path)))


(defn read-at [path callback-fn]
  (.once (fb-ref path) "value" #(callback-fn (.val %))))

(defn write-at [path value]
  (.set (fb-ref path) value))

(defn push-at
  ([path] (.push (fb-ref path)))
  ([path value] (.push (fb-ref path) value)))

(defn delete-at [path]
  (.remove (fb-ref path) (fn [e] (js/console.log "DELETE:" e))))


(defn is-user-equal [google-user firebase-user]
  (and
    firebase-user
    (some
      #(and (= (.-providerId %) js/firebase.auth.GoogleAuthProvider.PROVIDER_ID)
            (= (.-uid %) (.getId (.getBasicProfile google-user))))
      (:providerData firebase-user))))

(defn ^:export onSignIn [google-user]
  (when (not (is-user-equal google-user (<sub :firebase/current-user)))
    (.catch
      (.signInWithCredential
        (js/firebase.auth)
        (js/firebase.auth.GoogleAuthProvider.credential
          (.-id_token (.getAuthResponse google-user))))
      (fn [error]
        (js/alert error)))))


(defn on-auth []
  (.onAuthStateChanged
   (js/firebase.auth)
   (fn auth-state-changed [firebase-user]
     (let [uid (.-uid firebase-user)
           display-name (.-displayName firebase-user)
           photo-url (.-photoURL firebase-user)
           provider-data (.-providerData firebase-user)]
       (if uid
         (do
           (write-at ["users" uid "settings"]
                     #js {:photo-url photo-url
                          :display-name display-name
                          :email (-> provider-data first .-email)})
           (>evt [:firebase/current-user
                  {:photoURL photo-url
                   :displayName display-name
                   :email (-> provider-data first .-email)
                   :uid uid
                   :providerData provider-data}]))
         (>evt [:firebase/current-user nil]))))
   (fn auth-error [error]
     (js/alert error))))

(defn init-fb [firebase-app-info]
  (js/firebase.initializeApp firebase-app-info)
  (on-auth))

(defn google-sign-in []
  ;; TODO: use Credential for mobile.
  (.signInWithRedirect
    (js/firebase.auth.)
    (js/firebase.auth.GoogleAuthProvider.)))

(re-frame/reg-fx :firebase/google-sign-in google-sign-in)

(defn sign-out []
  ;; TODO: add then/error handlers
  (.signOut (js/firebase.auth))
  (>evt [:firebase/current-user nil]))

(re-frame/reg-fx :firebase/sign-out sign-out)

(defn- js->clj-tree [x]
  (-> (.val x)
      js->clj
      clojure.walk/keywordize-keys))

(defn- user-uid [app-db]
  (get-in app-db [:firebase/current-user :uid]))

(s/def ::fb-path (s/coll-of string? :into []))
(s/def ::domain #{:raw :public :user})
(s/def ::app-db #(= reagent.ratom/RAtom (type %)))

(defn- data-path
  "Expand the path to a FireBase datum.
  Domain can be :user, :public or :raw, defaulting to :user.
  :raw - Able to access the full Firebase tree (subject to
         security restrictions, of course.)
  :public - Stored under /public, and visible to all users.
  :user - Private to a user, stored under /users/<uid>/"
  [app-db path domain]
  {:pre [(utils/validate ::app-db app-db)
         (utils/validate ::fb-path path)
         (utils/validate ::domain domain)]}
  (let [root (case domain
               :raw []
               :public ["public"]
               :user ["users" (user-uid @app-db)])]
    (into root path)))

(def ^:private on-event-key :firebase/_internal_on_watcher)

(re-frame/reg-sub-raw
 :firebase/on
 (fn [app-db [_ path domain]]
   (let [app-db-root :firebase/firebase
         path        (data-path app-db path domain)
         app-db-path (into [app-db-root] (map keyword path))
         ref         (fb-ref path)]
     (.on ref "value" #(>evt [on-event-key app-db-path (js->clj-tree %)]))
     (rv/make-reaction
      (fn [] (get-in @app-db app-db-path []))
      :on-dispose #(do (.off ref)
                       (>evt [on-event-key app-db-path nil]))))))


;;; [TODO] Move this somewhere useful, along with tests, etc.
;;;        Also see, e.g. https://crossclj.info/clojure/dissoc-in.html
;;;          and https://dev.clojure.org/jira/browse/CLJ-1063
;;;          and https://stackoverflow.com/questions/14488150/how-to-write-a-dissoc-in-command-for-clojure
;;;        to see if we can steal any better code
(defn dissoc-in
  "Dissoc an interior node from a tree, and prune empty branches leading to it"
  [m ks]
  {:pre [(utils/validate map? m)
         (utils/validate coll? ks)]}
  (if (= 1 (count ks))
    (dissoc m (first ks))
    (let [but-last (butlast ks)
          m (update-in m but-last dissoc (last ks))]
      (if (empty? (keys (get-in m but-last)))
        (dissoc-in m but-last)
        m))))

(re-frame/reg-event-db
 on-event-key
 (fn [app-db [_ path value]]
   (if value
     (assoc-in app-db path value)
     (dissoc-in app-db path))))
