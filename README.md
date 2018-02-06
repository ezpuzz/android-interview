# TV / Movies

We want to build an app for retrieving information about TV shows and movies.

The requirements are:

1. Have a screen where the user can search for TV shows and movies by name
  - There should only be 1 input for both TV shows and movies
  - The search should be done in realm-time (the user doesn't have to submit the query)
  - There should be some type of limit on searches so that we don't do a search on every keystroke

2. Display the search results
  - The results of the search should be shown in a list
  - Each result should show:
    1. A thumbnail of the poster
    2. The name
    3. A brief overview
    4. Whether it is a TV show or movie

3. Display more info
  - Tapping on a result should show more info about the TV show or movie
  - This info should include whatever you feel a user would want to know about a TV show or movie

Design is left up to you, but keep it material themed.

The goal for now is to get a working app as fast as possible.

If you would like to take time to make the app more "production ready" as a take home assignment, please let you interviewer know.

## TMDb

We will use The Movie Database API (TMDb) to retrieve the information we need.

[You can find their documentation here](https://developers.themoviedb.org/3/getting-started/introduction).

Your interviewer will provide you with the necessary API key.