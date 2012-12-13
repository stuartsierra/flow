# Flow

Function definitions derived from graph declarations.

**ALPHA RELEASE**


## Introduction ##

I was at [Strange Loop 2012] in Saint Louis, where the [Prismatic]
team did a talk about their [Graph] library for building up complex
processes in a declarative style. At the time, Prismatic had not yet
released Graph as open-source, and I wanted something like it, so I
wrote my own.

[Strange Loop 2012]: https://thestrangeloop.com/
[Prismatic]: http://getprismatic.com/
[Graph]: http://blog.getprismatic.com/blog/2012/10/1/prismatics-graph-at-strange-loop.html

I also wanted to do something new with the [dependency graph] library
I wrote for [tools.namespace] 0.2.0. Flow turned out to be pretty easy
given a data structure to represent a DAG.

[dependency graph]: https://github.com/clojure/tools.namespace/blob/tools.namespace-0.2.0/src/main/clojure/clojure/tools/namespace/dependency.clj
[tools.namespace]: https://github.com/clojure/tools.namespace


## Releases ## 

Flow is on [Clojars](https://clojars.org/com.stuartsierra/flow)

Latest release is [0.0.1](https://github.com/stuartsierra/flow/tree/flow-0.0.1)

Leiningen dependency information:

    [com.stuartsierra/flow "0.0.1"]


## Usage ##

**Flow** is a small library for building up functions as discrete steps without specifying the order of those steps. This can help clarify functions that have lots of `let`-bindings. For example, this function:

    (defn process [alpha beta]
      (let [gamma (subprocess-a alpha beta)
            delta (subprocess-b alpha gamma)
            epsilon (subprocess-c gamma delta)]
        (subprocess-d gamma delta epsilon)))

Could be written as a Flow like this:

    (require [com.stuartsierra.flow :as flow])

    (def process-flow
      (flow/flow
        result  ([gamma delta epsilon]
                   (subprocess-d gamma delta epsilon))
        gamma   ([alpha beta]  (subprocess-a alpha beta))
        delta   ([alpha gamma] (subprocess-b alpha gamma))
        epsilon ([gamma delta] (subprocess-c gamma delta))))
          
The body of a Flow consists of pairs of an *output* and a *fntail*.

The **fntail** is a list with an argument vector and body just like
`fn`.

The **output** is a symbol binding for the return value the that
function.

Symbols that appear as both outputs and arguments to fntails create
dependency relationships. In the example above, the `result` output
depends on `gamma`, `delta`, and `epsilon`.

Symbols which appear in argument vectors but not as output-symbols are
the /inputs/ to the flow. In the example above, `alpha` and `beta` are
inputs.

The order of the pairs doesn't matter: you can use a symbol in an
argument vector before it is declared as an output.

For demonstration, I'll defined the subprocesses to be addition:

    (def subprocess-a +)
    (def subprocess-b +)
    (def subprocess-c +)
    (def subprocess-d +)

Once you have defined a Flow, you can execute it by providing a map of
inputs. You get back a map containing the final result plus all the
intermediate outputs computed along the way.

    (flow/run process-flow {:alpha 1 :beta 2} [:result])
    ;;=> {:result 14, :epsilon 7, :delta 4, :gamma 3,
    ;;    :alpha 1, :beta 2}

You can also specify which outputs you want. The Flow only computes as
much as it has to:

    (flow/run process-flow {:alpha 1 :beta 2} [:gamma :delta])
    ;;=> {:delta 4, :gamma 3, :alpha 1, :beta 2}

You can also override some or all of the outputs by providing them as
input:

    (flow/run process-flow {:alpha 1 :beta 2 :gamma 30} [:result])
    ;;=> {:result 122, :epsilon 61, :delta 31, :alpha 1,
    ;;    :beta 2, :gamma 30}


## Compiling Flows ##

If you know the input keys in advance, you can precompile a Flow into
a function which will run faster. Call the function with a map of
inputs, and you get a map of all the outputs:

    (def compute (flow/compile process-flow [:alpha :beta]))

    (compute {:alpha 1 :beta 2})
    ;;=> {:result 14, :epsilon 7, :delta 4, :gamma 3,
    ;;    :alpha 1, :beta 2}

As with `run`, you can optionally specify which outputs you want:

    (def compute-gamma
      (flow/compile process-flow [:alpha :beta] [:gamma]))

    (compute-gamma {:alpha 1 :beta 2})
    ;;=> {:gamma 3, :alpha 1, :beta 2}


## Visualing Flows ##

If you have the [Graphviz] program installed, you can generate
visualizations of a flow. The `dot` function prints out a
Graphviz-compatible representation of the flow:

    (flow/dot process-flow 'myflow)
    ;; digraph myflow {
    ;;    beta -> gamma ;
    ;;    gamma -> epsilon ;
    ;;    gamma -> result ;
    ;;    gamma -> delta ;
    ;;    alpha -> gamma ;
    ;;    alpha -> delta ;
    ;;    epsilon -> result ;
    ;;    delta -> epsilon ;
    ;;    delta -> result ;
    ;; }

Write that out to a file:

    (flow/write-dotfile process-flow 'myflow "myflow.dot")

Then run Graphviz:

    $ dot -Tpng -o myflow.png myflow.dot

And see the results:

![flow visualization](https://raw.github.com/stuartsierra/flow/master/myflow.png)

[Graphviz]: http://www.graphviz.org/


## Change Log ##

* Release [0.0.1](https://github.com/stuartsierra/flow/tree/flow-0.0.1) on 12-Oct-2012


## Copyright & License ##

Copyright (c) 2012 Stuart Sierra. All rights reserved. This program
and the accompanying materials are made available under the terms of
the Eclipse Public License v1.0 which accompanies this distribution,
and is available at http://www.eclipse.org/legal/epl-v10.html
