# Creating the Pom Cache in MySQL

Once the application has been deployed for the first time, you must populate the MySQL database searched by this application to identifying dependent repositories. This MySQL database contains a cache of all pom.xml files and associated repository metadata found on GitHub.

The initial data was retrieved from Google BigQuery, which has a very large public data set of github repos, and can be searched via the Google Cloud Console.

The github_repos public data set was used to carry out the search from, with the following query:

````
SELECT
  content.id,
  content.content,
  content.size,
  file.repo_name,
  file.path,
  file.ref
FROM
  `bigquery-public-data.github_repos.contents` AS content,
  `bigquery-public-data.github_repos.files` AS file
WHERE
  file.path LIKE '%pom%.xml'
  AND content.id = file.id
````


I ran this query and directed the results to a table in a dataset on google cloud. I then downloaded
the results as a collection of csv files, and imported those into my own MySQL database, by copying the csv files into the Docker container running the MySQL database, and then executing into the Docker container and running following command:

`mysqlimport -v --ignore-lines=1 --fields-terminated-by=','  --fields-enclosed-by='\"' --columns='bigtable_id,content,size,repo_name,path,ref' --local -u root -p github-pom /github-pom.csv`

If you do not wish to extract this data from Google BigQuery, you can use the [csv files](https://www.dropbox.com/sh/dx9f1n33v8mc00g/AAC2Pd6HvNot1W2LtsRq8G7ua?dl=0) I have already generated. Alternatively, a [MySQL dump](https://www.dropbox.com/s/0me2z5u00quuyk0/mysql-dump.sql?dl=0) of the full set of CSV files also exists.
