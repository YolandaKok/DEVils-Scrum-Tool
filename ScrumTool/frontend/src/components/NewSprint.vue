<template>
    <b-container class="Navigation" fluid>
        <navbar :dashboard="true"></navbar>
                <b-row>
            <p></p>
        </b-row>        <b-row>
            <p></p>
        </b-row>
        <b-row>
            <b-col align-self="start">
                <h2 class="pcsprint">Create New Sprint</h2>
            </b-col>
        </b-row>
        <br> <br>
        <b-row style="padding-right: 30px; padding-left: 30px">
            <b-col  class="text-center">
                <b-form>
                    <label class="pcsprint">Plan:</label>
                    <b-form-input class="input-style pcsprint" v-model="sprint.plan" type="text" placeholder="Type the project's plan"></b-form-input>
                    <b-row>
                        <p></p>
                    </b-row>
                    <label class="pcsprint">Goal:</label>
                    <b-form-input class="input-style pcsprint" v-model="sprint.goal" type="text" placeholder="Type the project's goal"></b-form-input>
                    <b-row>
                        <p></p>
                    </b-row>
                    <label class="pcsprint">Deadline Date:</label>
                    <div>
                      <b-form-input class="input-style pcsprint" id="inputLive" v-model.trim="sprint.deadlineDate" type="date" :state="dateState"></b-form-input>
                      <b-form-invalid-feedback id="inputLiveFeedback">
                      Sprint's Deadline cannot be a past or current date.
                      </b-form-invalid-feedback>
                    </div>

                    <!--<b-form-input class="input-style" v-model="sprint.deadlineDate" type="date"></b-form-input>-->
                </b-form>
            </b-col>
            <b-col>
                <b-row>
                    <b-col >
                        <h5 class="pcsprint">Select an Epic: </h5>
                        <select class="pcsprint" v-model="selectedValue" @click="loadStories(selectedValue.idPBI, selectedValue.title)">
                            <option disabled value="">Please select one</option>
                            <option v-for="item in epics" :value="item">{{item.title}}</option>
                        </select>
                    </b-col>
                    <b-col>
                        <div>
                            <h5 class="pcsprint" style="margin-top: 5px">Click to add a user story: </h5>
                            <b-form-select v-model="selected" :options="stories_options" class="mb-3 pcsprint" :select-size="4" style="min-width: 280px">
                            </b-form-select>
                        </div>
                    </b-col>
                </b-row>
                <b-row>
                    <p></p>
                </b-row>
                <b-row>
                    <p></p>
                </b-row>
                <b-row>
                    <b-col >
                    </b-col>
                    <b-col class="text-right">
                        <h5 class="pcsprint" style="font-size: 110%">User Stories for the new sprint: </h5>
                        <b-form-select v-model="toremove" :options="selected_stories" class="mb-3 pcsprint" :select-size="4" style="min-width: 280px">
                        </b-form-select>
                    </b-col>
                </b-row>
            </b-col>
          </b-row>
        <b-row>
            <b-col class="text-center pcsprint">
                <b-button style="margin-top: 40px; padding-left:100px; padding-right: 100px;" type="submit" size="lg" variant="success" @click="submit" :disabled="dateState===false">
                    Create
                </b-button>
            </b-col>
        </b-row>
    </b-container>
</template>

<script>
  import Navbar from "./Navbar.vue"
  import axios from 'axios'
  import json from '../assets/team.json'
  export default {
    name: "NewSprint",
    components: {
      navbar: Navbar,
    },

    computed: {

    dateState () {
    var mindate = new Date();
        var dd = ("0" + mindate.getDate()).slice(-2);
        var mm = ("0" + (mindate.getMonth()+1)).slice(-2);
        var yyyy = mindate.getFullYear();
        mindate=yyyy+"-"+mm+"-"+dd;
        if (this.sprint.deadlineDate==='') return null;
        return this.sprint.deadlineDate>mindate
      }

    },

    data() {
      return {
        sprint: {
          deadlineDate: '',
          goal: '',
          plan: '',
          isCurrent: true,
          numSprint: 0,
          Project_id: 0
        },
        toremove: null,
        selected: null,
        selectedValue: null,
        selectedvalue: null,
        options: [],
        stories_options: [],
        selected_stories: [],
        pbis_list: [],
        epics: [],
        items: [{
          text: 'Home',
          href: '/#/'
        }, {
          text: 'Create New Sprint',
          active: true
        }]
      }
    },
    methods: {
      submit () {
        const self = this;
        console.log(self.sprint);
        let new_sprint = {deadlineDate: self.sprint.deadlineDate, Project_id: self.sprint.Project_id,
          goal: self.sprint.goal, plan: self.sprint.plan
          , isCurrent: self.sprint.isCurrent, numSprint: self.sprint.numSprint+1};
        console.log("paparia");
        console.log(self.sprint.numSprint);

        let config = {
          headers: { "auth": localStorage.getItem('auth_token'), "Content-Type":'application/json' }
        }
        // Call to create the new sprint

        axios.post(self.$url + 'users/' +localStorage.getItem('userId')+'/projects/'+ self.sprint.Project_id + '/sprints', new_sprint, config)
          .then(function (response) {
              for(var i = 0; i < self.selected_stories.length; i++) {
                self.pbis_list.push({idPBI: self.selected_stories[i].value, Sprint_id: response.data.Sprint_id, Project_id: self.sprint.Project_id})
              }
              console.log(self.pbis_list)
              console.log(self.sprint.Project_id)
            axios.patch(self.$url + 'users/' + localStorage.getItem('userId') + '/projects/' + self.sprint.Project_id + '/pbis', self.pbis_list, config)
              .then(function (response) {
                // Debugging
                console.log("xoxo")
                self.$router.push({path: '/project/' + self.sprint.Project_id + '/overview/'})
              })
              .catch(function(error){
                console.log(error)
              })
            // Call to update the pbis !
            // Sprint_id, idPBI
          })
          .catch(function (error) {
            console.log(error);
          })
      },
      loadStories(idPBI, title) {
        console.log(idPBI);
        console.log(title);
        // Call the other rest endpoint to get the stories of each epic
        const self = this;
        axios.get(self.$url + 'users/' +localStorage.getItem('userId')+'/projects/'+ self.sprint.Project_id + '/pbis?epicId=' + idPBI, {
          headers: { "auth": localStorage.getItem('auth_token') }
        })
          .then(function (response) {
            if (response.data.error) {
              if (response.data.error == "Unauthorized") {
                console.log("Unauthorized");
              }
            }
            if (response.data.results) {
              self.stories_options = []
              self.stories = response.data.results
              let id;
              let title;
              // Create an array of object
              for(let i = 0; i < self.stories.length; i++) {
                title = self.stories[i].title
                id = self.stories[i].idPBI
                self.stories_options.push({value: id, text: title})
              }
              console.log(self.stories)
            }
          })
          .catch(function (error) {
            console.log(error);
          })
      },
      getEpics () {
        const self = this;
        axios.get(self.$url + 'users/' +localStorage.getItem('userId')+'/projects/'+ self.sprint.Project_id + '/pbis?isEpic=true', {
          headers: { "auth": localStorage.getItem('auth_token') }
        })
          .then(function (response) {
            if (response.data.error) {
              if (response.data.error == "Unauthorized") {
                console.log("Unauthorized");
              }
            }
            if (response.data.results) {
              self.epics = response.data.results;
              let id;
              let title;
              // Create an array of object
              for(let i = 0; i < self.epics.length; i++) {
                title = self.epics[i].title;
                id = self.epics[i].idPBI;
                self.options.push({value: id, text: title});
              }

              console.log(self.epics);
            }
          })
          .catch(function (error) {
            console.log(error);
          })
      },
      selectedStories(value) {
        const self = this;
        console.log("selected");
        // filter using the pbi id
        var result = self.stories_options.filter(function( obj ) {
          return obj.value == value;
        });
        var find_pbi = self.selected_stories.find(function (find_pbi) { return find_pbi.value === value; });
        if (find_pbi == undefined) {
          self.selected_stories.push({value: value, text: result[0].text});
        }
      },
      removeStory(value){
        const self = this;
        console.log(value);
        console.log("deselected");
        for ( var x in self.selected_stories){
          if ( self.selected_stories[x].value == value){
            var index = x;
            break;
          }
        }
        console.log("position");
        console.log(index);
        if( index >= 0){
          self.selected_stories.splice(index,1);
        }
      }
    },
    mounted() {
      this.sprint.Project_id = this.$route.params.id;      // Receive projectId from url parameters
      //this.sprint.Project_id = 5;
      this.sprint.numSprint  = this.$route.query.newSprintNum;   // Receive new numSprint (+1 from the previous one)
      if( isNaN(this.sprint.numSprint)){
        console.log("dada");
        this.sprint.numSprint=0;
      }
      //this.sprint.numSprint = 8;
      if (this.sprint.Project_id == null || this.sprint.numSprint == null) {
        console.log("Error: Add correct parameters (projectId and newSprintNum) in url");
      }
      this.getEpics();
    },
    watch: {
      selected: function (value) {
        this.selectedStories(value);
        console.log(value);
      },
      toremove: function (value) {
        this.removeStory(value);
        console.log(value);
      }
    }
  }
</script>

<style scoped>
  @import url('https://fonts.googleapis.com/css?family=Merienda');
  @import url('https://fonts.googleapis.com/css?family=VT323');
  @import url('https://fonts.googleapis.com/css?family=Quicksand');

  .ptitle{
    font-family: Merienda;
  }

  .pdeadline{
    font-family: VT323;font-size:24px; margin-top: 1%;
  }

  .pcsprint{
    font-family: Quicksand;
  }
  .line {
    width: 99%;
    font-size: 1px;
    line-height: 2px;
    background-color: lavender;
    margin-top: -6px;
    margin-bottom: 10px;
  }

  .Navigation {
    position: absolute;
    top: 0;
    left: 0;
  }

  .container-fluid {
    padding-right: 0;
    padding-left: 0;
  }

  .form-frame {
    margin-top: 50px;
    border-radius: 0px;
    background: #4568DC;  /* fallback for old browsers */
    background: linear-gradient(-90deg, #d3d3d3, #d5d5d5); /* W3C, IE 10+/ Edge, Firefox 16+, Chrome 26+, Opera 12+, Safari 7+ */
    box-shadow: 20px 20px 50px #aaaaaa;
  }
  .container-fluid {

  }

  .input-style {
    border-radius: 5px;
    padding: 15px;
  }

  select {
    padding-top: 10px;
    padding-bottom: 10px;
    padding-left: 20px;
    padding-right: 20px;
    margin-top: 10px;
    border-radius: 5px;
    background-color: #f3f3f3;
  }

  select.option {
    padding-top: 10px;
    padding-bottom: 10px;
    padding-left: 20px;
    padding-right: 20px;
    margin-top: 10px;
    border-radius: 5px;
    background-color: red;
  }

  .text-enhancement {
    font-family: 'Merienda', cursive;
  }

</style>
