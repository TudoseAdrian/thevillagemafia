const path = require('path');

process.env.GOOGLE_APPLICATION_CREDENTIALS = path.resolve(__dirname, 'service-account.json');

const client = require('firebase-tools');

client.deploy({
  project: 'thevillagemafia',
  site: 'thevillagemafia',
  only: 'hosting',
  cwd: __dirname
}).then(function() {
  console.log('Successfully deployed to Firebase Hosting!');
}).catch(function(err) {
  console.error('Error deploying to Firebase:', err);
});
