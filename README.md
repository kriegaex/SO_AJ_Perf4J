# Perf4J Maven example

I created this sample project as a reaction to a [StackOverflow question](http://stackoverflow.com/q/43377648/1082681)
in order to demonstrate how [Perf4J](https://github.com/perf4j/perf4j), a useful, but old and unmaintained performance
timing & reporting framework, can be used from Maven with both AspectJ-based approaches mentioned in its
[developer guide](https://web.archive.org/web/20150508124059/http://perf4j.codehaus.org/devguide.html):
  * compile-time weaving (CTW)
  * load-time weaving (LTW)

The developer guide only shows how to manually set up a project, but not how to automate builds with Maven. Beside the
fact that the old project Codehaus homepage no longer exists and can only be reached via WayBack Machine, the CTW
documentation is based on logging-framework-specific libraries unavailable on Maven Central which have to be manually
downloaded and integrated into the build. Those extra libs are basically the full lib minus timing aspects for all but
one logging framework.

I am showing a way to also use the full library for CTW by means of XML configuration similar to the LTW approach. It
is a mostly unknown fact that a subset of _aop.xml_ features such as `<aspect .../>` declarations can also be used for
CTW via the undocumented AspectJ compiler (ajc) parameter
[`-xmlConfigured`](https://bugs.eclipse.org/bugs/show_bug.cgi?id=455014). This way we can easily select which aspects
should be applied during CTW. We can even use the same `META-INF/aop.xml` file as for LTW. We only need to be careful
because we cannot expect weaver options like `<include within="de.scrum_master..*"/>` to function for CTW. CTW with
_aop.xml_ always behaves like `<include within="*"/>`. But at least we can select which aspects should be compiled into
the application code.

## How to build

### CTW

By default I have activated both Java logging to console and Log4J logging into a file, both of which I am showing
here. Please adjust `aop.xml`, Maven dependencies and config files to your logging framework of choice, this is just
an example.

    $ rm perfStats.log
    $ mvn clean verify

    (...)
    [INFO] --- aspectj-maven-plugin:1.10:compile (default) @ perf4j-example ---
    [INFO] Showing AJC message detail for messages of types: [error, warning, fail]
    [INFO] Join point 'method-execution(void de.scrum_master.so.Application.doSomething())' in Type 'de.scrum_master.so.Application' (Application.java:13) advised by around advice from 'org.perf4j.log4j.aop.TimingAspect' (perf4j-0.9.16.jar!AbstractTimingAspect.class(from AbstractTimingAspect.java))
    [INFO] Join point 'method-execution(void de.scrum_master.so.Application.doSomething())' in Type 'de.scrum_master.so.Application' (Application.java:13) advised by around advice from 'org.perf4j.javalog.aop.TimingAspect' (perf4j-0.9.16.jar!AbstractTimingAspect.class(from AbstractTimingAspect.java))
    (...)
    [INFO] --- exec-maven-plugin:1.4.0:java (run-application-ctw) @ perf4j-example ---
    Waiting for 250 ms ... done
    Apr 14, 2017 11:23:03 AM org.perf4j.javalog.JavaLogStopWatch log
    INFORMATION: start[1492161783037] time[317] tag[doSomething]
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    (...)

    $ cat perfStats.log

    Performance Statistics   2017-04-14 11:25:30 - 2017-04-14 11:25:40
    Tag                                                  Avg(ms)         Min         Max     Std Dev       Count
    doSomething                                            249,0         249         249         0,0           1

### LTW

In order to activate the LTW Maven profile it is sufficient to define a property also named `LTW`.

    $ rm perfStats.log
    $ mvn -DLTW clean verify

    (...)
    [INFO] --- exec-maven-plugin:1.4.0:exec (run-application-ltw) @ perf4j-example ---
    [AppClassLoader@18b4aac2] info AspectJ Weaver Version 1.8.10 built on Monday Dec 12, 2016 at 19:07:48 GMT
    [AppClassLoader@18b4aac2] info register classloader sun.misc.Launcher$AppClassLoader@18b4aac2
    [AppClassLoader@18b4aac2] info using configuration /C:/Users/Alexander/Documents/java-src/SO_AJ_Perf4J/target/classes/META-INF/aop.xml
    [AppClassLoader@18b4aac2] info register aspect org.perf4j.javalog.aop.TimingAspect
    [AppClassLoader@18b4aac2] info register aspect org.perf4j.log4j.aop.TimingAspect
    [AppClassLoader@18b4aac2] weaveinfo Join point 'method-execution(void de.scrum_master.so.Application.doSomething())' in Type 'de.scrum_master.so.Application' (Application.java:14) advised by around advice from 'org.perf4j.log4j.aop.TimingAspect' (AbstractTimingAspect.java)
    [AppClassLoader@18b4aac2] weaveinfo Join point 'method-execution(void de.scrum_master.so.Application.doSomething())' in Type 'de.scrum_master.so.Application' (Application.java:14) advised by around advice from 'org.perf4j.javalog.aop.TimingAspect' (AbstractTimingAspect.java)
    Waiting for 250 ms ... done
    Apr 14, 2017 11:29:39 AM org.perf4j.javalog.JavaLogStopWatch log
    INFORMATION: start[1492162179515] time[325] tag[doSomething]
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    (...)

    $ cat perfStats.log
    Performance Statistics   2017-04-14 11:29:30 - 2017-04-14 11:29:40
    Tag                                                  Avg(ms)         Min         Max     Std Dev       Count
    doSomething                                            250,0         250         250         0,0           1

## Configuration differences between CTW and LTW

Both builds have many things in common as can be seen when inspecting _pom.xml_. Noteworthy differences are*
  * For CTW we need [AspectJ Maven Plugin](http://www.mojohaus.org/aspectj-maven-plugin/index.html), for LTW
    Maven Compiler is enough.
  * On the other hand, for running the CTW code it is enough to just put the AspectJ runtime _aspectjrt.jar_ on the
    classpath, which is very simple: `java -cp /path/to/aspectjrt;... ...`
  * For running the LTW code, we need the AspectJ weaver _aspectjweaver.jar_ on the command line as a Java agent:
    `java -javaagent:/path/to/aspectjweaver.jar ...`. This also needs to be taken into account when trying to create a
    run configurations in your IDE of choice, e.g. IntelliJ IDEA. Eclipse with AJDT (AspectJ Development Tools)
    installed even knows a special LTW run config type, there it is a bit easier.
  * Both types of run configurations can be inspected in _pom.xml_. Just compare the different Maven Exec plugin
    configurations for both profiles.
  * As a little goodie I added the OneJAR plugin which packages the whole application and all its dependencies into a
    single JAR which can just be started via
    
    `java -jar target/perf4j-example-1.0-SNAPSHOT.one-jar.jar`
    
    for the CTW case and via
    
    `java -javaagent:c:/Users/Alexander/.m2/repository/org/aspectj/aspectjweaver/1.8.10/aspectjweaver-1.8.10.jar -jar target/perf4j-example-1.0-SNAPSHOT.one-jar.jar`
    
    for the LTW case (adjust AspectJ weaver path to your local Maven repository).
