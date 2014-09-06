# noisy

TODO

## Usage


### Paint Noise

``` clojure
(require '[noisy.core :as c])
(c/paint (c/murmur-random-generator) 200 200)
```

![](doc/noise.png)

### Scale Noise

```clojure
(c/paint (-> (c/murmur-random-generator)
             (c/floor)
             (c/scale 10)) 200 200)
```

![](doc/scaled_noise.png)

## License

TODO

Copyright © 2014 Roman Flammer
