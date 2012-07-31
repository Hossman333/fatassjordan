<?php

##### SETUP #####
require 'Slim/Slim.php';
require 'Slim/Views/MustacheView.php';
MustacheView::$mustacheDirectory = 'Slim/Views';
$app = new Slim(array(
    'view' => 'MustacheView'
));


##### ROUTES #####
$app->get('/', function() use($app) {
	$app->render('home.mustache');
});

$app->get('/tweets', fetchTweets($app), function() use($app) {
	$app->response()->header("Content-Type", "application/json");
    echo $app->twitter_data;
});


##### ROUTE MIDDLEWARE #####
function fetchTweets($app) {
	$file = getcwd() . "/fatass.json";
	$valid_cache = check_for_valid_cache($file);

	if($valid_cache) {
		$data = $valid_cache[0];
	} else {
		$data = pull_from_twitter("fatasswill");
		file_put_contents($file, $data);
	}

	$app->twitter_data = $data;
}

function check_for_valid_cache($file) {
	if(file_exists($file) && time() - 360*15 < filemtime($file)) {
		return file($file);
	}
}

function pull_from_twitter($username) {
	$twitter_response = json_decode(file_get_contents("https://api.twitter.com/1/statuses/user_timeline.json?include_entities=false&include_rts=false&exclude_replies=true&trim_user=true&screen_name=".$username));
	$data = array();

	foreach($twitter_response as $tweet) {
		array_push($data, array(
			"id" => $tweet->id_str,
			"date" => date("M j, Y", strtotime($tweet->created_at)),
			"time" => date("g:i:s A", strtotime($tweet->created_at)),
			"weight" => (float)$tweet->text,
			"timestamp" => strtotime($tweet->created_at) * 1000,
			"formattedWeight" => number_format((float)$tweet->text, 1)
		));
	}

	return json_encode($data);
}

$app->run();