# Example For a Centralized Error Response Handling and a Custom Error Page in Quarkus

This is an example project for the article [Centralized Error Response Handling and a Custom Error Page in Quarkus](https://marcelkliemannel.com/articles/2021/centralized-error-handling-and-a-custom-error-page-in-quarkus/).

There are two subprojects, one for the __global error response filter__ which can be started via: 
````shell
./gradlew :global-error-response-filter:quarkusDev
````
and one for the __global exception mapper__, which can be started via:
````shell
./gradlew :global-exception-mapper:quarkusDev
````

If we now call the endpoint `http://localhost:8080/bad-request`, we can see that our custom error handling gets invoked. Also, the `Accept` header matching can be tested via:
```shell
> curl --header "Accept: text/plain" http://localhost:8080/forbidden
Error 403 (Forbidden)

Action not allowed.
```

```shell
> curl --header "Accept: application/json" http://localhost:8080/forbidden
[{"status":403,"title":"Forbidden","detail":"Action not allowed."}]
```

```shell
> curl --header "Accept: text/html" http://localhost:8080/forbidden
<!doctype html>
<html lang="en">
<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Error 403 (Forbidden)</title>
</head>
<body>
	<h1>Error 403 (Forbidden)</h1>
	<p>Action not allowed.</p>
</body>
</html>
```