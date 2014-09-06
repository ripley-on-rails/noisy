# noisy

TODO

## Usage

### Paint Noise

``` clojure
(require '[noisy.core :as c])
(c/paint2d (c/murmur-random-generator) 200 200)
```

![](doc/noise2d.png)

``` clojure
(require '[noisy.core :as c])
(c/paint1d (c/murmur-random-generator) 400 100)
```

![](doc/noise1d.png)

### Scale Noise


```clojure
(c/paint2d (-> (c/murmur-random-generator)
               (c/floor)
               (c/scale 10)) 200 200)
```

... Minecraft anyone?

![](doc/scaled_noise.png)

```clojure
(c/paint1d (-> (c/murmur-random-generator)
               (c/floor)
               (c/scale 10)) 400 100)
```

![](doc/scaled_1dnoise.png)

### Perlin Noise

#### Generic example

```clojure
(c/paint2d (-> (c/perlin (c/murmur-random-generator) c/fade1)
               (c/scale 10)) 200 200)
```

![Perlin Noise](doc/perlin_noise.png)

```clojure
(c/paint1d (-> (c/perlin (c/murmur-random-generator) c/fade1)
               (c/scale 40)) 400 100)
```

![](doc/perlin_noise_1d.png)

#### Close-up

```clojure
(c/paint2d (-> (c/perlin (c/murmur-random-generator) c/fade1)
               (c/scale 100)) 200 200)
```

![Close-up](doc/perlin_closeup.png)

#### Linear Interpolation (Yikes!)

```clojure
(c/paint2d (-> (c/perlin (c/murmur-random-generator) c/linear-interpolation)
               (c/scale 10)) 200 200)
```

![Perlin Noise using linear interpolation... Yikes!](doc/perlin_linear.png)

```clojure
(c/paint1d (-> (c/perlin (c/murmur-random-generator) c/linear-interpolation)
               (c/scale 40)) 400 100)
```

![](doc/perlin_linear_1d.png)

## Utilities

### Grid

```clojure
(c/paint2d (-> (c/perlin (c/murmur-random-generator) c/fade1)
               (c/scale 40)) 200 200 :grid 40 :file "grid2d.png")
```

![](doc/grid2d.png)

```clojure
(c/paint1d (-> (c/perlin (c/murmur-random-generator) c/fade1)
               (c/scale 40)) 400 100 :grid 40 :file "grid1d.png")
```

![](doc/grid1d.png)

## TODOs
 - Add caching middleware
 - Add tiling
 - Add offset
 - Add paint3d:
   - export depth many images
   - export to animated GIF(?)

## License

TODO

Copyright © 2014 Roman Flammer
