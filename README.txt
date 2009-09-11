Copyright 2009 Daiji Takamori

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

-------------------------------
Selenium HTML Client
-------------------------------

Summary:

A test driver program to execute Selenese HTML-based Selenium tests against a remote Selenium Remote Control server.

I wrote this as a quick prototype implementation because there wasn't a way to do this, 
there was demand for this in the forums, and no one else seemed either interested or capable of developing this.
Currently there is only support in Selenium RC for executing Selenese HTML directly from the machine where 
the browser is located (which means it is not truly a "remote" usage in that case).

This driver basically wraps around the Selenium RC Java client driver.  
It also implements the additional functionality that is specific to Selenese HTML
and not typically supported in RC clients.  It should be capable of running most or all of the Selenese HTML tests.

The implementation might not be ideal and was initially implemented as proof of concept,
but it should suffice for the basic needs of those who need this sort of support.

-------------------------------
Usage:
-------------------------------

--host hostname             Specify Selenium server (default: localhost
--port portnumber           Specify Selenium server port (default: 4444)
--browser browserspec       Specify Selenium browser (default: *opera)
--out outputfilename        Specify a file for output
--baseurl testurlbase       Specify the base URL for any relative URLs
--test testfile             Execute a single test file
--testsuite testsuitefile   Execute a test suite file
--verbose, -v               Set verbose mode on
--help, -h                  Display usage

-------------------------------
Author:
-------------------------------
dt_02138@yahoo.com

