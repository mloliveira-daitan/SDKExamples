java-bandwidth-examples
=======================

There are two examples of how to use the bandwidth sdk to implement an event server using Java servlets, HelloFlipperServlet and ChaosCorpServlet (in progress).


You'll need the following prereqs to run these:

You want a Bandwidth Application Platform account (https://catapult.inetwork.com/pages/signup.jsf)
You'll need Maven installed (http://maven.apache.org/download.cgi#Installation)
And the jdk installed (http://docs.oracle.com/javase/7/docs/webnotes/install/index.html)

Building the Example app

First we want to clone the repo and build the app
	
	git clone 	https://github.com/bandwidthcom/java-bandwidth-examples.git
	cd java-bandwidth-examples
	mvn clean install

Now we want to deploy it to a running web server. The steps to deploy to Heroku and AWS are included below.

Heroku Deployment

For the Heroku deployment, you'll need to insure that you have a Heroku account and that the Heroku toolbelt has been installed for your environment (https://devcenter.heroku.com/articles/heroku-command)

The steps to deploy the sample app are:

1. Set up the git repo 
2. Create a new Heroku app 
3. Configure the Heroku app
4. Push the project to Heroku 

Step 1 - Set up the git repo

From the java-bandwidth-examples directory
	
	git init
	git add .
	git commit -m "my bandwidth example"

Step 2 - Create a new Heroku app
	
	heroku apps:create
	git remote add heroku git@heroku.com:new-app.git

Step 3 - Configure the new Heroku app

	heroku config:set BANDWIDTH_USER_ID='your user id from the app platform'
	heroku config:set BANDWIDTH_API_TOKEN='your api token from the app platform'
	heroku config:set BANDWIDTH_API_SECRET='your api secret from the app platform'

	heroku config:set BANDWIDTH_API_SECRET='the number you want the example app to call'

Note that your Bandwidth user id, api token and api secret are obtained by logging into the app platform UI and going to the Account tab.

Step 4 - Push the project to heroku

	git push heroku master
	heroku ps:scale web=1

You can now verify that the app is 	successfully deployed - 
	
	heroku logs -tail --app <heroku-app-name> 

And verify the app is running 
	heroku 	open

To setup the phone number see the Routing the Phone Number section below.


AWS Installation

For the AWS installation, you'll want to have an AWS account and signed into the Management Console

The steps to deploy the example app are:

1. 

To setup the phone number see the Routing the Phone Number section below.

Routing the Phone Number

Now that the app is deployed, we want it to handle all inbound phone calls. To do that we'll want to:

 - Provision a phone number on the app platform
 - Create an Application and associate it with the phone number

- Provisioning a phone number on the app platform

 	Log in to the App Platform (https://catapult.inetwork.com/pages/login.jsf)
 	Go to the My Numbers tab and select Get New Numbers
 	Fill in the form for a number in your area and click the Search button
 	Select one of the numbers and click the Get Numbers button

- Creating an Application and associating the phone number with it

	Go to the My Apps tab and click the Create New link
	Fill in the name and the Call URL field, using the following:
		<heroku-app-name>.herokuapp.com/helloflipper&callLeg=incoming
	Click the Save button
	Select the number you provisioned
	Click the Add Numbers button

Now if you call the number, it will be routed to the demo app. You can trace the progression in the 



