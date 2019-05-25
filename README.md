# PEX Java example

Read list of image urls from input file and find 3 most prevalent colors in the RGB scheme in hexadecimal format (#000000 - #FFFFFF) in each image, and write the result into a CSV file in a form of url,color,color,color.

## Installation

Compile with maven: ``mvn clean package``.

## Image processing 

Run ``java -jar target/pex-1.0-SNAPSHOT-jar-with-dependencies.jar <input_file> <csv_file>``. Result is written to CSV file as url,color,color,color.
Original input.txt file is included in project root.
