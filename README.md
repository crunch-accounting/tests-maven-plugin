# crunch-services-maven-plugin

Add this Maven plugin to your project to enable automatic generation of lots of stuff...


----

## How to configure in your own project:

Add the following to your POM:

    <plugin>
        <groupId>uk.co.crunch</groupId>
        <artifactId>crunch-services-maven-plugin</artifactId>
        <version>2.9.0</version>
    </plugin>


## How to use in your own project:

Build as normal.


## Going further:

This project could be extended to generate various other types of boilerplate - or perform additional compile-time validation - to make local development more efficient.


## Features

### Helm chart

#### Add more manifests

* Create a folder called `helm-templates` on the root of the maven project that uses this plugin.
* Create a `Makefile` on `helm-templates/Makefile` with, at least 2 targets, `build-all` and `push-all`
* Drop as many `*.yaml` Helm manifests on the `helm-templates` folder as you need