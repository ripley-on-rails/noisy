(ns noisy.core
  (:import [java.awt.image BufferedImage]
           [java.awt Color]
           [java.util Random]
           [javax.imageio ImageIO]
           [java.io File]
           [com.google.common.hash Hashing])
  (:require [clojure.math.numeric-tower :as math]))

(defn- normalize-int [i]
  (cond
   (pos? i) (double (/ i Integer/MAX_VALUE))
   (neg? i) (- (double (/ i Integer/MIN_VALUE)))
   :else i))

;;; implicit functions
(defn murmur-random-generator
  "Returns a fn that given ints as input returns a deterministic PRN
   Double in [-1.0, 1.0]."
  ([]
     (murmur-random-generator 0))
  ([seed]
     (let [hashing (Hashing/murmur3_32 seed)]
       (fn [& vals]
         (let [hasher (.newHasher hashing)]
           (doseq [val vals]
             (.putInt hasher val))
           (-> (.asInt (.hash hasher))
               normalize-int))))))

;;; modifiers
(defn scale [source & factors]
  (fn [& coords]
    {:pre [(or (= 1 (count factors))
               (= (count factors)
                  (count coords)))]}
    (apply source
           (map #(/ % %2)
                coords
                (if (= 1 (count factors))
                  (repeat (first factors))
                  factors)))))

(defn floor [source]
  (fn [& coords]
    (apply source (map #(int (math/floor %)) coords))))

;;; RGBA functions
(defn- gray
  "Returns a gray-scale rgb representation given [-1.0, 1.0] as input"
  [i]
  (let [j (int (* 128 (inc i)))]
    (.getRGB (Color. j j j))))

(defn paint [generator width height & opts]
  {:pre [width height generator]}
  (let [file-name (or (first opts) "image.png")
        image (BufferedImage. width height BufferedImage/TYPE_INT_ARGB)]
    (doseq [x (range width)
            y (range height)]
      (.setRGB image x y (gray (generator x y))))
    (ImageIO/write image "png" (File. file-name))))

#_
(
 (require '[noisy.core :as c])
 (c/paint (-> (c/murmur-random-generator)
              (c/floor)
              (c/scale 10))
          200 200)
 )
