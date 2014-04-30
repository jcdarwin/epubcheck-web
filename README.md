# EpubCheck Web Archive build tool

This project allows you to create a web archive and serve it using [Apache Tomcat](http://tomcat.apache.org/), such that 
you can provide validation of EPUB files using [EpubCheck](https://github.com/IDPF/epubcheck) via a web service.

The notes below presume a Mac build environment, but it should be relatively easy to adapt them to other environments.

* Clone the EpubCheck repo

		git clone https://github.com/IDPF/epubcheck.git

* Checkout the tagged release we wish to build

		cd epubcheck/
		git tag -l
		git checkout tags/v3.0.1

* Install maven, and use it to build the release

		brew install maven
		mvn install

* At the root level, checkout the EpubCheck .war code (i.e. this repo)

		git checkout epubcheck-web

* Make some changes to the build file, `epubcheck-web\build-war.xml`, 
specifically include the correct version number, as found in the name of .jar file (e.g.)

		<property name="version" value="3.0.1" />

* If you want to customise the HTML and CSS used for the web service, then make the amendments to the code in `epubcheck-web\http_root`

* Install ant and use it to build the `epubcheck.war`:

		brew install ant

		ant -f build-war.xml

* Install the `.war` and restart tomcat

		cd /usr/local/tomcat/webapps
		cp /tmp/epubcheck/epubcheck-web/dist/epubcheck-3.0.1.war epubcheck.war
		/etc/init.d/tomcat start

Once Tomcat’s restarted, you should be able to see the epubcheck service at [http://localhost:8080/epubcheck](http://localhost:8080/epubcheck).

On my system I use Apache to proxy requests from port 80 to 8080, so I can make it available as I do at [http://mebooks.co.nz/epubcheck/](http://mebooks.co.nz/epubcheck/)


