# Visualizing Reviews 
An alternate visualization of TripAdvisor Reviews. Uses Stanford Parser for POS Tagging & Dependency Parsing i.e
we isolate the Nouns & their related Adjectives.
Uses a Sentiment Lexicon from http://www.cs.uic.edu/~liub/FBS/sentiment-analysis.html to classify the Adjectives as
either Positive or Negative.
Each co-occurance of a Noun ("Subject") & class of adjectives (Posivite/Negative), increments it's Weight.
This outputs an Excel file that drives the Visualization.
Visualization is achieved via graphics from http://raw.densitydesign.org/.
Sample input file, intermediate Excel file & the final output are attached.
