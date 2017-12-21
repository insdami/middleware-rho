# middleware-rho
# SharedSwaggerSupportInstance
It's has the right functionality but the `swagger.json` file is not the expected one, meaning that it has only one of the two endpoints

# MountTwice
It generates the swagger.json file correctly but `Service1` is mounted twice, so if the order in which the services are mounted is swapped `Service1` will be mounted without the middleware.

