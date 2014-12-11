<!doctype html>

<html>
<head>

</head>

<body>

<H1>Congratulations! You've deployed the Bandwidth Sample Java app</H1>

Now you want to configuration a Bandwidth phone number so that your example app can handle the incoming call. To do this, you'll
want to do the following:

- Provision a phone number on the app platform
- Create an Application and associate it with the phone number


<B>Provisioning a phone number on the app platform</B>

Log in to the App Platform (https://catapult.inetwork.com/pages/login.jsf)
Go to the My Numbers tab and select Get New Numbers
Fill in the form for a number in your area and click the Search button
Select one of the numbers and click the Get Numbers button

<B>Creating an Application and associating the phone number with it</B>

Go to the My Apps tab and click the Create New link
Fill in the name and the Call URL field, using the following:

<heroku-app-name>.herokuapp.com/helloflipper?callLeg=incoming
    Click the Save button
    Select the number you provisioned
    Click the Add Numbers button

    Now if you call the number, it will be routed to the demo app. You can trace the call flow in the log output of your app.


</body>
</html>
