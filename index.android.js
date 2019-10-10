import {
  NativeModules,
  DeviceEventEmitter
} from 'react-native';

const samsungHealth = NativeModules.RNSamsungHealth;

class RNSamsungHealth {
  authorize( callback) {
    samsungHealth.connect(
      [samsungHealth.STEP_COUNT],
      (msg) => { callback(msg, false); },
      (res) => { callback(false, res); },
    );
  }

  stop() {
    samsungHealth.disconnect();
  }

  getDailyStepCount(options, callback) {
    let startDate = options.startDate != undefined ? options.startDate : (new Date()).setHours(0,0,0,0);
    let endDate = options.endDate != undefined ? options.endDate : (new Date()).valueOf();


    samsungHealth.readStepCount(startDate, endDate,
      (msg) => { callback(msg, false); },
      (res) => {
          if (res.length>0) {
              var resData = res.map(function(dev) {
                  var obj = {};
                  obj.source = dev.source.name;
                  obj.data = this.buildDailySteps(dev.data);
                  obj.sourceDetail = dev.source;
                  return obj;
                }, this);


              callback(false, resData);
          } else {
              callback("There is no any steps data for this period", false);
          }
      }
    );
  }

  getWalkingRunningDistance(options, callback) {
    let startDate = options.startDate != undefined ? options.startDate : (new Date()).setHours(0,0,0,0);
    let endDate = options.endDate != undefined ? options.endDate : (new Date()).valueOf();


    samsungHealth.readStepCount(startDate, endDate,
      (msg) => { callback(msg, false); },
      (res) => {
          if (res.length>0) {
              var resData = res.map(function(dev) {
                  var obj = {};
                  obj.source = dev.source.name;
                  obj.data = this.buildDailyDistance(dev.data);
                  obj.sourceDetail = dev.source;
                  return obj;
                }, this);


              callback(false, resData);
          } else {
              callback("There is not any steps distance data for this period", false);
          }
      }
    );
  }

  getWeight(options, callback) {
    let startDate = options.startDate != undefined ? options.startDate : (new Date()).setHours(0,0,0,0);
    let endDate = options.endDate != undefined ? options.endDate : (new Date()).valueOf();

    samsungHealth.readWeight(startDate, endDate,
      (msg) => { callback(msg, false); },
      (res) => {
        callback(false, res);
      }
    );
  }

  getSleep(options, callback) {
    let startDate = options.startDate != undefined ? options.startDate : (new Date()).setHours(0,0,0,0);
    let endDate = options.endDate != undefined ? options.endDate : (new Date()).valueOf();

    samsungHealth.readSleep(startDate, endDate,
      (msg) => { callback(msg, false); },
      (res) => {
        callback(false, res);
      }
    );
  }



  getHeartRate(options, callback) {
    let startDate = options.startDate != undefined ? options.startDate : (new Date()).setHours(0,0,0,0);
    let endDate = options.endDate != undefined ? options.endDate : (new Date()).valueOf();

    samsungHealth.readHeartRate(startDate, endDate,
      (msg) => { callback(msg, false); },
      (res) => {
        callback(false, res);
      }
    );
  }

  getBodyTemprature(options, callback) {
    let startDate = options.startDate != undefined ? options.startDate : (new Date()).setHours(0,0,0,0);
    let endDate = options.endDate != undefined ? options.endDate : (new Date()).valueOf();

    samsungHealth.readBodyTemprature(startDate, endDate,
      (msg) => { callback(msg, false); },
      (res) => {
        callback(false, res);
      }
    );
  }



  getBloodPressure(options, callback) {
    let startDate = options.startDate != undefined ? options.startDate : (new Date()).setHours(0,0,0,0);
    let endDate = options.endDate != undefined ? options.endDate : (new Date()).valueOf();

    samsungHealth.readBloodPressure(startDate, endDate,
      (msg) => { callback(msg, false); },
      (res) => {
        callback(false, res);
      }
    );
  }

  getHeight(options, callback) {

    let startDate = options.startDate != undefined ? options.startDate : (new Date()).setHours(0,0,0,0);
    let endDate = options.endDate != undefined ? options.endDate : (new Date()).valueOf();

    samsungHealth.readHeight(startDate, endDate,
      (msg) => { callback(msg, false); },
      (res) => {
        callback(false, res);
      }
    );
  }
  
  getCholesterol(options, callback) {

    let startDate = options.startDate != undefined ? options.startDate : (new Date()).setHours(0,0,0,0);
    let endDate = options.endDate != undefined ? options.endDate : (new Date()).valueOf();

    samsungHealth.readCholesterol(startDate, endDate,
      (msg) => { callback(msg, false); },
      (res) => {
        callback(false, res);
      }
    );
  }

  getDateOfBirth(callback) {
    samsungHealth.readDateOfBirth(
      (msg) => { callback(msg, false); },
      (res) => {
        callback(false, res);
      }
    );
  }

  getGender(callback) {
    samsungHealth.readGender(
      (msg) => { callback(msg, false); },
      (res) => {
        callback(false, res);
      }
    );
  }

  getLatestBodyFatPercentage(options, callback) {
    let startDate = options.startDate != undefined ? options.startDate : (new Date()).setHours(0,0,0,0);
    let endDate = options.endDate != undefined ? options.endDate : (new Date()).valueOf();

    samsungHealth.readBodyFatPercentage(startDate, endDate,
      (msg) => { callback(msg, false); },
      (res) => {
        callback(false, res);
      }
    );
  }

  getWorkoutSamples(options, callback) {
    let startDate = options.startDate != undefined ? options.startDate : (new Date()).setHours(0, 0, 0, 0);
    let endDate = options.endDate != undefined ? options.endDate : (new Date()).valueOf();

    samsungHealth.readWorkoutSamples(startDate, endDate,
      (msg) => { callback(msg, false); },
      (res) => {
        if (res.length > 0) {
          var resData = res.map(function (dev) {
            var obj = {};
            obj.source = dev.source.name;
            obj.data = dev.data.map(function (sample) {
              var workout = {};
              workout.calorie = sample.calorie;
              workout.distance = sample.distance;
              workout.start_time = sample.start_time;
              workout.end_time = sample.end_time;
              workout.duration = sample.duration;
              workout.timeOffset = sample.timeOffset;
              workout.exercise_type = this.convertActivityIDtoSlug(sample);
              return sample;
            }, this);
            obj.sourceDetail = dev.source;
            return obj;
          }, this);
          callback(false, resData);
        } else {
          callback("There is no workout data for this period", false);
        }
      }
    );
  }



  usubscribeListeners() {
    DeviceEventEmitter.removeAllListeners();
  }

  buildDailySteps(data)
  {
      var results = [];
      for(var step of data) {
          var date = step.start_time !== undefined ? step.start_time : step.day_time;

          results.push({steps:step.count, date:date,  calorie:step.calorie})
        
      }
      return results;
  }

  buildDailyDistance(data)
  {
      var results = [];
      for(var step of data) {
        var date = step.start_time !== undefined ? step.start_time : step.day_time;
        
        results.push({distance:step.distance, date:date, calorie:step.calorie})
        
      }
      return results;
  }

  convertActivityIDtoSlug(sample) {
    if (!sample.exercise_type || sample.exercise_type === 0) {
      return 'IndividualSport';
    }
    
    return activityIDToSlug[sample.exercise_type] ? activityIDToSlug[sample.exercise_type] : 'IndividualSport';
  }

  mergeResult(res)
  {
      results = {}
      for(var dev of res)
      {
          if (!(dev.sourceDetail.group in results)) {
              results[dev.sourceDetail.group] = {
                  source: dev.source,
                  sourceDetail: { group: dev.sourceDetail.group },
                  stepsDate: {}
              };
          }

          let group = results[dev.sourceDetail.group];

          for (var step of dev.steps) {
              if (!(step.date in group.stepsDate)) {
                  group.stepsDate[step.date] = 0;
              }

              group.stepsDate[step.date] += step.value;
          }
      }

      results2 = [];
      for(var index in results) {
          let group = results[index];
          var steps = [];
          for(var date in group.stepsDate) {
              steps.push({
                date: date,
                value: group.stepsDate[date]
              });
          }
          group.steps = steps.sort((a,b) => a.date < b.date ? -1 : 1);
          delete group.stepsDate;

          results2.push(group);
      }

      return results2;
  }
}

const activityIDToSlug = {
  1001: 'Walking',
  1002: 'Running',
  2001: 'Baseball',
  2002: 'Softball',
  2003: 'Cricket',
  3001: 'Golf',
  3002: 'Billiards',
  3003: 'Bowling',
  4001: 'Hockey',
  4002: 'Rugby',
  4003: 'Basketball',
  4004: 'Football',
  4005: 'Handball',
  4006: 'Soccer',
  5001: 'Volleyball',
  5002: 'BeachVolleyball',
  6001: 'Squash',
  6002: 'Tennis',
  6003: 'Badminton',
  6004: 'TableTennis',
  6005: 'Racquetball',
  7001: 'TaiChi',
  7002: 'Boxing',
  7003: 'MartialArts',
  8001: 'Ballet',
  8002: 'Dancing',
  8003: 'BallroomDancing',
  9001: 'Pilates',
  9002: 'Yoga',
  10001: 'Stretching',
  10002: 'JumpRope',
  10003: 'HulaHooping',
  10004: 'PushUps',
  10005: 'PullUps',
  10006: 'SitUps',
  10007: 'CircuitTraining',
  10008: 'MountainClimbers',
  10009: 'JumpingJacks',
  10010: 'Burpee',
  10011: 'BenchPress',
  10012: 'Squats',
  10013: 'Lunges',
  10014: 'LegPresses',
  10015: 'LegExtensions',
  10016: 'LegCurls',
  10017: 'BackExtensions',
  10018: 'LatPullDowns',
  10019: 'Deadlifts',
  10020: 'ShoulderPresses',
  10021: 'FrontRaises',
  10022: 'LateralRaises',
  10023: 'Crunches',
  10024: 'LegRaises',
  10025: 'Plank',
  10026: 'ArmCurls',
  10027: 'ArmExtensions',
  11001: 'InlineSkating',
  11002: 'HangGliding',
  11003: 'PistolShooting',
  11004: 'Archery',
  11005: 'HorsebackRiding',
  11007: 'Cycling',
  11008: 'FlyingDisc',
  11009: 'RollerSkating',
  12001: 'Aerobics',
  13001: 'Hiking',
  13002: 'RockClimbing',
  13003: 'Backpacking',
  13004: 'MountainBiking',
  13005: 'Orienteering',
  14001: 'Swimming',
  14002: 'Aquarobics',
  14003: 'Canoeing',
  14004: 'Sailing',
  14005: 'ScubaDiving',
  14006: 'Snorkeling',
  14007: 'Kayaking',
  14008: 'Kitesurfing',
  14009: 'Rafting',
  14010: 'RowingMachine',
  14011: 'Windsurfing',
  14012: 'Yachting',
  14013: 'WaterSkiing',
  15001: 'StepMachine',
  15002: 'WeightMachine',
  15003: 'ExerciseBike',
  15004: 'RowingMachine',
  15005: 'Treadmill',
  15006: 'EllipticalTrainer',
  16001: 'CrossCountrySkiing',
  16002: 'Skiing',
  16003: 'IceDancing',
  16004: 'IceSkating',
  16006: 'IceHockey',
  16007: 'Snowboarding',
  16008: 'AlpineSkiing',
  16009: 'Snowshoeing'
};


export default new RNSamsungHealth();
