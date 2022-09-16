<template>
  <div>
    <table class="table table-hover mt-2">
      <thead class="thead-dark">
      <tr>
        <th scope="col">Студент</th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="s in students"
          @click="makeActive(s)"
          @dblclick="editStudent(s)"
          v-bind:class="{ 'table-active': s.active }">
        <td><span>{{ s.name }}</span>
          <a class="btn float-right"
             data-toggle="collapse"
             role="button">
            Свойства
          </a>
        </td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<script lang="ts">
import {Component, Inject, Vue} from 'vue-property-decorator';

interface StudentDto {
  name: string;
  active: boolean;
}
@Component
export default class StudentTable extends Vue {
  private students: StudentDto[] = [{name: 'Foo Bar', active: true}];
  private activeStudent?: StudentDto;

  public refresh() {
    $.ajax({
      url: '/admin/student/all',
    }).done((students: StudentDto[]) => {
      this.students = students;
    }).fail((xhr) => {
      const title = 'Не удалось получить список студентов:';
      // this.alertDialog().show(title, xhr.statusText);
    });
  }

  public makeActive(student: StudentDto) {
    student.active = true;
    if (this.activeStudent) {
      this.activeStudent.active = false;
    }
    this.activeStudent = student;
    this.$forceUpdate();
  }

  public editStudent(s: StudentDto) {
    console.log(s);
  }
}
</script>

<style scoped>

</style>
