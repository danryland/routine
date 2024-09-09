<template>
  <q-page :class="{ 'edit-mode': editMode }">

    <q-header elevated>
      <q-toolbar class="q-px-md q-py-lg">
        <q-toolbar-title class="text-center text-weight-bold">
          Routine
        </q-toolbar-title>
      </q-toolbar>
    </q-header>

    <div class="container">

      <div class="fixed-side">
        <div class="task task-title">
          <template v-if="!editMode">
            <p>Date</p>
          </template>
          <template v-else>
            <p>Edit tasks</p>
          </template>
        </div>
        <div v-for="task in routine.tasks" :key="task" class="task">
          <div class="move">
            <i class="fa-regular fa-sharp fa-grip-dots-vertical"></i>
          </div>

          <template v-if="editMode">
            <div class="grow">
              <template v-if="task.target">
                <q-btn round @click="toggleTaskType(task)">
                  <i class="fa-xl fa-regular fa-square-check text-white"></i>
                </q-btn>
                <input type="number" v-model="task.target" placeholder="-" min="2" step="1" pattern="\d*" />
              </template>
              <template v-else>
                <q-btn round @click="toggleTaskType(task)">
                  <i class="fa-xl fa-regular fa-circle-dot text-white"></i>
                </q-btn>
              </template>
            </div>
          </template>
          <input placeholder="Task" v-model="task.name" type="text" />
          <button v-if="editMode" class="remove" @click="removeTask(task)">
            <span>
              <i class="fa-regular fa-sharp fa-times"></i>
            </span>
          </button>
        </div>
      </div>

      <div class="scrollable-side">
        <div class="tasks" v-for="date in routine.dates" :key="date">

          <div class="task task-stat">
            <template v-if="getCompletionPercentage(date) === 100">
              <span class="completion-100">
                <img src="../assets/img/icon-star.svg" alt="Star Icon" />
              </span>
            </template>
            <template v-else-if="getCompletionPercentage(date) >= 76">
              <span class="completion-76-99">{{ getCompletionPercentage(date) }}%</span>
            </template>
            <template v-else-if="getCompletionPercentage(date) >= 51">
              <span class="completion-51-75">{{ getCompletionPercentage(date) }}%</span>
            </template>
            <template v-else-if="getCompletionPercentage(date) >= 26">
              <span class="completion-26-50">{{ getCompletionPercentage(date) }}%</span>
            </template>
            <template v-else-if="getCompletionPercentage(date) >= 1">
              <span class="completion-1-25">{{ getCompletionPercentage(date) }}%</span>
            </template>
            <template v-else>
              <span class="completion-0">0%</span>
            </template>
          </div>

          <div :class="['task task-title', { 'active': isToday(date) }]">
            <p>{{ new Date(date).toLocaleDateString('en-US', { weekday: 'short' }) }}</p>
            <p>{{ new Date(date).getDate() }}</p>
          </div>
          <div class="task" v-for="task in routine.tasks" :key="task">
            <!-- Binary Task Button -->
            <button v-if="isBinaryTask(task)" @click="toggleBinaryTask(task, date)"
              :class="{ completed: task.completed.includes(date) }">
              <span v-if="task.completed.includes(date)"><i class="fa-solid fa-sharp fa-check"></i></span>
              <span v-else><i class="fa-solid fa-sharp fa-check"></i></span>
            </button>

            <!-- Multiple Completion Task Button -->
            <div class="wrapper" v-else>
              <button @click="incrementMultipleTask(task, date)"
                :class="[{ completed: getCompletionTimes(task, date) === task.target }, `segment segment-${getCompletionTimes(task, date)}/${task.target}`]">
                <template v-if="getCompletionTimes(task, date) === task.target">
                  <span>
                    <i class="fa-solid fa-sharp fa-check"></i>
                  </span>
                </template>
                <template v-else>
                  <span>{{ getCompletionTimes(task, date) }}</span>
                </template>
              </button>

              <q-circular-progress rounded :value="(getCompletionTimes(task, date) / task.target) * 100" size="48px"
                :thickness="0.2" track-color="grey-5" class="" />

            </div>

          </div>
        </div>
      </div>

      <pre v-if="debug">{{ routine }}</pre>

    </div>

    <q-footer class="bg-custom">
      <q-toolbar class="q-py-md justify-evenly">
        <q-btn class="size" size="lg" round icon="fa-sharp fa-regular fa-pen" @click="editMode = !editMode" no-caps />
        <q-btn size="lg" round icon="fa-sharp fa-solid fa-plus" @click="addTask()" />
      </q-toolbar>
    </q-footer>

  </q-page>
</template>


<script setup>
import { ref, reactive, onMounted, watch } from 'vue';
import { date } from 'quasar';
import { Preferences } from '@capacitor/preferences';

defineOptions({
  name: 'IndexPage'
});

const debug = ref(false);

const routine = reactive({
  dates: [],
  tasks: []
});

const editMode = ref(false);

const storeRoutine = async () => {
  await Preferences.set({
    key: 'routine',
    value: JSON.stringify(routine)
  });
};

const isToday = (date) => {
  const today = new Date();
  const [day, month, year] = date.split(' ');
  const givenDate = new Date(`${month} ${day}, ${year}`);
  return today.toDateString() === givenDate.toDateString();
};

const loadRoutine = async () => {
  const { value } = await Preferences.get({ key: 'routine' });
  if (value) {
    const storedRoutine = JSON.parse(value);
    routine.dates = storedRoutine.dates || [];
    routine.tasks = storedRoutine.tasks || [];
  }
};

onMounted(async () => {
  await loadRoutine();
  addMissingDaysToRoutine();
  const now = new Date();
  const delay = (60 - now.getMinutes()) * 60 * 1000 - now.getSeconds() * 1000 - now.getMilliseconds();
  setTimeout(() => {
    addMissingDaysToRoutine();
    setInterval(addMissingDaysToRoutine, 3600000); // Check every hour (3600000 ms)
  }, delay);
});

watch(routine, storeRoutine, { deep: true });

if (debug.value) {
  routine.dates = [
    "23 Aug 2024", "24 Aug 2024", "25 Aug 2024", "26 Aug 2024", "27 Aug 2024", "28 Aug 2024",
    "29 Aug 2024"
  ];
  routine.tasks = [
    { name: "Teeth AM", completed: ["26 Aug 2024", "27 Aug 2024"] },
    { name: "Face AM", completed: ["26 Aug 2024", "27 Aug 2024", "28 Aug 2024"] },
    { name: "SPF", completed: ["28 Aug 2024"] },
    { name: "Shower", completed: ["26 Aug 2024", "27 Aug 2024"] },
    { name: "Tongue", completed: ["26 Aug 2024", "27 Aug 2024"] },
    { name: "Daily Greens", completed: ["26 Aug 2024", "27 Aug 2024"] },
    {
      name: "Water x6",
      completed: [
        { date: "26 Aug 2024", times: 1 },
        { date: "27 Aug 2024", times: 2 },
        { date: "28 Aug 2024", times: 6 }
      ],
      target: 6
    }
  ];
}

const addMissingDaysToRoutine = () => {
  const lastDate = routine.dates.length > 0 ? routine.dates[routine.dates.length - 1] : null;
  const today = new Date();
  const formattedToday = date.formatDate(today, 'D MMM YYYY');

  if (lastDate) {
    let currentDate = new Date(lastDate);
    currentDate.setDate(currentDate.getDate() + 1);

    while (currentDate <= today) {
      routine.dates.push(date.formatDate(currentDate, 'D MMM YYYY'));
      currentDate.setDate(currentDate.getDate() + 1);
    }
  } else {
    routine.dates.push(formattedToday);
  }
};

const isBinaryTask = (task) => {
  return !task.target;
};

const toggleBinaryTask = (task, date) => {
  const index = task.completed.indexOf(date);
  if (index > -1) {
    task.completed.splice(index, 1);
  } else {
    task.completed.push(date);
  }
};

const toggleTaskType = (task) => {
  if (!task.target) {
    // Convert to multiple completion task
    task.completed = task.completed.map(date => (typeof date === 'string' ? { date: date, times: 0 } : date));
    task.target = 2;
  } else {
    // Convert to binary task
    task.completed = task.completed.map(entry => entry.date || entry);
    delete task.target;
  }
};

const incrementMultipleTask = (task, date) => {
  const entry = task.completed.find((e) => e.date === date);
  if (entry) {
    entry.times = (entry.times + 1) % (task.target + 1);
  } else {
    task.completed.push({ date: date, times: 1 });
  }
};

const getCompletionTimes = (task, date) => {
  const entry = task.completed.find((e) => e.date === date);
  return entry ? entry.times : 0;
};

const addTask = () => {
  routine.tasks.push({ name: "Task", completed: [] });
};

const removeTask = (task) => {
  const index = routine.tasks.indexOf(task);
  if (index > -1) {
    routine.tasks.splice(index, 1);
  }
};

const getCompletionPercentage = (date) => {
  const totalTasks = routine.tasks.length;
  if (totalTasks === 0) return 0;

  const completedTasks = routine.tasks.reduce((count, task) => {
    if (isBinaryTask(task)) {
      return count + (task.completed.includes(date) ? 1 : 0);
    } else {
      const entry = task.completed.find((e) => e.date === date);
      return count + (entry && entry.times === task.target ? 1 : 0);
    }
  }, 0);

  return Math.ceil((completedTasks / totalTasks) * 100);
};

</script>

<style lang="scss">
body {
  font-family: "Avenir Next";
  background: linear-gradient(0deg, rgba(0, 0, 0, 0.72) 0%, rgba(0, 0, 0, 0.72) 100%), #4C0519;
}

.q-header {
  border-bottom: 1px solid #4C0519;
  background-color: #150107;
}

.bg-custom {
  border-top: 1px solid #4C0519;
  background-color: #150107;
}

pre {
  color: white;
}

.container {
  padding-top: 8px;
  position: relative;
}

.fixed-side {
  position: absolute;
  left: 0px;
  width: 200px;

  -webkit-overflow-scrolling: touch;
  scroll-behavior: smooth;

  background: linear-gradient(0deg, rgba(0, 0, 0, 0.72) 0%, rgba(0, 0, 0, 0.72) 100%), #4C0519;
  color: white;

  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  z-index: 10;

  &::after {
    content: "";
    position: absolute;
    top: 0px;
    width: 10px;
    opacity: 0.4;
    background: linear-gradient(90deg, #150107 0%, rgba(21, 1, 7, 0.00) 96.09%);
  }
}

.scrollable-side {
  padding-left: 204px;
  padding-right: 12px;
  white-space: nowrap;
  overflow-x: scroll;
  display: flex;
  min-height: calc($rowHeight*2);
  margin-bottom: $rowHeight;
}

.tasks {
  display: inline-block;
  margin: 0px 4px;

  .task {
    padding: 0px;
    margin-bottom: 8px;

    &-title {
      display: flex;
      flex-direction: column;
      gap: 0px;
      justify-content: center;
      width: 64px;

      &.active {
        opacity: 1;
      }

      p {
        flex: none;
        align-items: center;
      }
    }

    &-stat {
      display: flex;
      flex-direction: column;
      gap: 0px;
      justify-content: center;
      width: 64px;

      margin-bottom: 0px;
      height: 40px;
      margin-top: 8px;

      p {
        flex: none;
        align-items: center;
      }
    }
  }
}

.fixed-side {
  .task-title {
    margin-top: 48px;
  }
}

.task {
  display: flex;
  height: $rowHeight;
  padding: 0px 16px 0px 0px;
  box-sizing: border-box;
  justify-content: flex-end;
  align-items: center;
  gap: 0px;
  align-self: stretch;

  color: white;

  &-title {
    opacity: 0.5;
  }

  p,
  input {
    display: flex;
    flex-direction: column;
    justify-content: center;
    flex: 1 0 0;
    align-self: stretch;
    overflow: hidden;
    text-align: right;
    text-overflow: ellipsis;
    //white-space: nowrap;
    font-size: 16px;
    font-style: normal;
    font-weight: 700;
    line-height: normal;
    margin: 0px;
  }

  input {
    background-color: transparent;
    box-shadow: none;
    border: none;
    width: 100%;
    color: white;
    transition: .3s ease-in-out all;

    &:focus {
      outline: none;
      background-color: rgba(white, 0.08);
      padding: 0px 10px;
      border-radius: 8px;
    }
  }
}

.grow {
  flex: 1 0 0;
  display: flex;
}

// .items {
//   &-activity {
//     position: fixed;
//     left: 0px;
//     width: 150px;

//     display: flex;
//     flex-direction: column;
//     align-items: flex-start;
//     gap: 8px;

//     .item {
//       display: flex;
//       height: $rowHeight;
//       padding: 0px 24px;
//       box-sizing: border-box;
//       justify-content: flex-end;
//       align-items: center;
//       gap: 8px;
//       align-self: stretch;

//       &-title {
//         opacity: 0.5;
//       }

//       p {
//         display: flex;
//         flex-direction: column;
//         justify-content: center;
//         flex: 1 0 0;
//         align-self: stretch;
//         overflow: hidden;
//         text-align: right;
//         text-overflow: ellipsis;
//         //white-space: nowrap;
//         font-size: 16px;
//         font-style: normal;
//         font-weight: 700;
//         line-height: normal;
//         margin: 0px;
//       }
//     }
//   }

//   &-progress {
//     display: flex;
//     flex-direction: column;
//     align-items: flex-start;
//     gap: 8px;

//     .item {
//       display: flex;
//       width: 64px;
//       height: 64px;
//       padding: 0px 13px 0px 12px;
//       justify-content: center;
//       align-items: center;

//       &-title {
//         display: flex;
//         flex-direction: column;
//         justify-content: center;
//         flex-shrink: 0;
//         text-align: center;
//         font-size: 16px;
//         font-style: normal;
//         font-weight: 700;
//         line-height: 18px;
//         opacity: 0.5;
//       }

//       p {
//         margin-bottom: 0px;
//       }
//     }
//   }
// }


.q-circular-progress__circle {
  color: #F43F5E !important;
}

.q-circular-progress__track {
  color: #4C0519 !important;
}

.wrapper {
  display: flex;
  width: $rowHeight;
  height: $rowHeight;
  position: relative;

  .q-circular-progress {
    position: absolute;
    top: 0px;
    left: 0px;
    right: 0px;
    bottom: 0px;
    margin: 8px;
    z-index: -1;
  }
}


button {
  display: flex;
  width: $rowHeight;
  height: $rowHeight;
  justify-content: center;
  align-items: center;
  background: transparent;
  border-radius: 8px;
  border: 2px solid #4C0519;
  overflow: hidden;
  color: #F43F5E;
  transition: .3s ease-in-out all;

  i {
    transition: .3s ease-in-out all;
    font-size: 40px;
    color: #4C0519;
  }

  &:hover {
    cursor: pointer;
    border-color: #F43F5E;

    i {
      color: #F43F5E;
    }
  }

  &.completed {
    background: #F43F5E;
    border-color: #F43F5E;

    i {
      color: white;
    }

    &:hover {
      i {
        color: white;
      }
    }


  }

  &.remove {
    border: none;

    &:hover {
      i {
        opacity: 1;
      }
    }

    i {
      transition: .3s ease-in-out all;
      font-size: 24px;
      color: white;
      opacity: 0.18;
    }
  }
}

.settings {
  padding-right: 8px;
  justify-content: space-between;

  button.edit {
    border: none;

    &:hover {
      i {
        opacity: 1;
      }
    }

    i {
      transition: .3s ease-in-out all;
      font-size: 24px;
      color: white;
      opacity: 0.40;
    }
  }

  button.add {
    border: none;
    width: auto;

    &:hover {
      i {
        opacity: 1;
      }
    }

    i {
      transition: .3s ease-in-out all;
      font-size: 24px;
      color: white;
      opacity: 0.40;
    }
  }
}

.q-footer {
  button {
    i {
      color: white !important;
    }

    &.size {
      .q-icon {
        font-size: 1.5em;
      }
    }
  }
}

.fixed-side {
  transition: .3s ease-in-out width;

  .grow {
    max-width: 100px;
  }
}

.edit-mode {
  .fixed-side {
    width: 91%;
  }
}


.move {
  display: flex;
  width: $rowHeight;
  height: $rowHeight;
  justify-content: center;
  align-items: center;
  border-radius: 8px;
  color: white;
  transition: .3s ease-in-out all;
  font-size: 24px;

  &:hover {
    cursor: grab;
  }
}

.completion-76-99 {
  font-weight: bold;
  color: lighten(#059669, 16);
}

.completion-51-75 {
  font-weight: bold;
  color: #059669;
}

.completion-26-50 {
  opacity: 0.8;
  color: #F43F5E;
}

.completion-1-25 {
  opacity: 0.6;
  color: #F43F5E;
}

.completion-0 {
  opacity: 0.4;
}
</style>
