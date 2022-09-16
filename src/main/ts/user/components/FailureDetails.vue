<template>
  <form v-if="hasFailure">
    <div class="form-group text-left">
      <label class="bmd-label-static" for="error-message">Сообщение об ошибке</label>
      <div id="error-message">
        <div v-for="line in errorLines()">
          {{ line }}
        </div>
      </div>
    </div>
    <div class="form-group">
      <div class="text-left">
        <label class="bmd-label-static" for="result-rows">Первые строки вашего результата</label>
      </div>
      <table id="result-rows" class="table table-sm table-fixed">
        <thead class="thead-light">
        <tr>
          <th v-for="column in columnNames">{{ column }}</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="row in rows">
          <td v-for="column in columnNames">
            {{ row[column] || 'NULL' }}
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </form>
</template>

<script lang="ts" src="./FailureDetails.ts">
</script>
