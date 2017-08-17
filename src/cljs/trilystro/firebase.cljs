;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.firebase
  (:require
   [clojure.string :as str]
   [reagent.core :as reagent]
   [sodium.chrome-utils :as chrome]))


;;; Built on ideas and code from
;;; http://timothypratley.blogspot.co.il/2016/07/reacting-to-changes-with-firebase-and.html
;;; and https://github.com/timothypratley/voterx



(defn fb-ref [path]
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


(defonce user-atom (reagent/atom nil))

(defn is-user-equal [google-user firebase-user]
  (and
    firebase-user
    (some
      #(and (= (.-providerId %) js/firebase.auth.GoogleAuthProvider.PROVIDER_ID)
            (= (.-uid %) (.getId (.getBasicProfile google-user))))
      (:providerData firebase-user))))

(defn ^:export onSignIn [google-user]
  (when (not (is-user-equal google-user @user-atom))
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
           (reset! user-atom {:photoURL photo-url
                              :displayName display-name
                              :uid uid
                              :providerData provider-data}))
         (when @user-atom
           (reset! user-atom nil)))))
   (fn auth-error [error]
     (js/alert error))))

(defn init-fb [firebase-app-info]
  (js/firebase.initializeApp firebase-app-info)
  (on-auth))

(defn sign-in []
  ;; TODO: use Credential for mobile.
  (.signInWithRedirect
    (js/firebase.auth.)
    (js/firebase.auth.GoogleAuthProvider.)))

(defn sign-out []
  ;; TODO: add then/error handlers
  (.signOut (js/firebase.auth))
  (reset! user nil))
